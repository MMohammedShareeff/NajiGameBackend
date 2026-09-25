package com.naji.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naji.openai.AiServiceException.Attempt;
import com.naji.openai.AiServiceException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiChatClient {

    private static final Logger log = LoggerFactory.getLogger(AiChatClient.class);
    private static final int DEFAULT_MAX_TOKENS = 1000;
    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>");
    private static final Pattern RETRY_HINT =
            Pattern.compile("(?i)(?:retryDelay\"?\\s*:\\s*\"?|try again in\\s+)(\\d+(?:\\.\\d+)?)s");

    private final AiProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Block> blocks = new ConcurrentHashMap<>();

    public AiChatClient(AiProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private record Block(Instant until, Attempt attempt) {
    }

    private static final class CallFailed extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final transient Attempt attempt;

        CallFailed(Attempt attempt) {
            super(attempt.describe(), null, false, false);
            this.attempt = attempt;
        }
    }

    public String chat(String prompt) {
        return chat(prompt, DEFAULT_MAX_TOKENS);
    }

    public String chat(String prompt, int maxTokens) {
        List<AiProperties.Resolved> chain = props.activeChain();
        if (chain.isEmpty()) {
            throw AiServiceException.notConfigured();
        }

        List<Attempt> attempts = new ArrayList<>();
        for (AiProperties.Resolved provider : chain) {
            Block block = blocks.get(provider.name());
            Instant now = Instant.now();
            if (block != null) {
                if (block.until().isAfter(now)) {
                    attempts.add(block.attempt().withRetryAfter(secondsLeft(block.until(), now)));
                    continue;
                }
                blocks.remove(provider.name());
            }

            long started = System.nanoTime();
            try {
                String answer = call(provider, prompt, maxTokens);
                log.info("AI answered via {} ({}) in {} ms", provider.name(), provider.model(),
                        (System.nanoTime() - started) / 1_000_000);
                return answer;
            } catch (CallFailed failed) {
                attempts.add(failed.attempt);
                coolDown(provider, failed.attempt);
            }
        }
        throw AiServiceException.allFailed(attempts);
    }

    private String call(AiProperties.Resolved provider, String prompt, int maxTokens) {
        HttpRequest request;
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", provider.model(),
                    "max_tokens", maxTokens,
                    "messages", List.of(Map.of("role", "user", "content", prompt))));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(provider.url()))
                    .timeout(Duration.ofSeconds(Math.max(1, props.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (!provider.apiKey().isEmpty()) {
                builder.header("Authorization", "Bearer " + provider.apiKey());
            }
            request = builder.build();
        } catch (Exception e) {
            throw fail(provider, Reason.BAD_REQUEST, null, "could not build the request: " + e);
        }

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw fail(provider, Reason.TIMEOUT, null, e.toString());
        } catch (IOException e) {
            throw fail(provider, Reason.NETWORK, null, e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw fail(provider, Reason.NETWORK, null, "interrupted");
        }

        int status = response.statusCode();
        if (status == 200) {
            return readAnswer(provider, response.body());
        }
        String detail = errorDetail(response.body());
        Long retryAfter = retryAfterSeconds(response, response.body());
        throw fail(provider, classify(status, detail), retryAfter, "HTTP " + status + ": " + detail);
    }

    private String readAnswer(AiProperties.Resolved provider, String body) {
        String text;
        try {
            JsonNode content = mapper.readTree(body).path("choices").path(0).path("message").path("content");
            if (content.isTextual()) {
                text = content.asText();
            } else if (content.isArray()) {
                StringBuilder sb = new StringBuilder();
                content.forEach(part -> sb.append(part.path("text").asText("")));
                text = sb.toString();
            } else {
                text = "";
            }
        } catch (Exception e) {
            throw fail(provider, Reason.BAD_RESPONSE, null, "unreadable JSON: " + e.getMessage());
        }
        text = THINK_BLOCK.matcher(text).replaceAll("").trim();
        if (text.isEmpty()) {
            throw fail(provider, Reason.BAD_RESPONSE, null, "the answer was empty");
        }
        return text;
    }

    static Reason classify(int status, String detail) {
        String d = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
        boolean keyProblem = d.contains("api key") || d.contains("api_key") || d.contains("apikey");

        if (status == 401 || status == 403) {
            return Reason.INVALID_KEY;
        }
        if (status == 400 && keyProblem) {
            return Reason.INVALID_KEY;
        }
        if (status == 402) {
            return Reason.QUOTA_EXHAUSTED;
        }
        if (status == 404) {
            return Reason.MODEL_NOT_FOUND;
        }
        if (status == 408) {
            return Reason.TIMEOUT;
        }
        if (status == 429) {
            return d.contains("insufficient_quota") || d.contains("billing")
                    ? Reason.QUOTA_EXHAUSTED : Reason.RATE_LIMIT;
        }
        if (status == 400 || status == 422) {
            return Reason.BAD_REQUEST;
        }
        if (status >= 500) {
            return Reason.PROVIDER_DOWN;
        }
        return Reason.BAD_RESPONSE;
    }

    private String errorDetail(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root != null && root.isArray() && root.size() > 0) {
                root = root.get(0);
            }
            JsonNode error = root == null ? null : root.get("error");
            if (error != null) {
                if (error.isTextual()) {
                    return clip(error.asText());
                }
                return clip((error.path("message").asText("") + " " + error.path("code").asText("")
                        + " " + error.path("type").asText("")).trim());
            }
        } catch (Exception ignored) {
        }
        return clip(body);
    }

    private static Long retryAfterSeconds(HttpResponse<String> response, String body) {
        try {
            String header = response.headers().firstValue("retry-after").orElse(null);
            if (header != null) {
                return (long) Math.ceil(Double.parseDouble(header.trim()));
            }
        } catch (NumberFormatException ignored) {
        }
        if (body != null) {
            Matcher m = RETRY_HINT.matcher(body);
            if (m.find()) {
                return (long) Math.ceil(Double.parseDouble(m.group(1)));
            }
        }
        return null;
    }


    private void coolDown(AiProperties.Resolved provider, Attempt attempt) {
        long seconds = switch (attempt.reason()) {
            case RATE_LIMIT -> attempt.retryAfterSeconds() != null ? Math.min(attempt.retryAfterSeconds(), 600) : 30;
            case QUOTA_EXHAUSTED ->
                    attempt.retryAfterSeconds() != null ? Math.min(attempt.retryAfterSeconds(), 600) : 300;
            case INVALID_KEY, MODEL_NOT_FOUND -> 60;
            case PROVIDER_DOWN, TIMEOUT, NETWORK -> 10;
            case BAD_REQUEST, BAD_RESPONSE -> 0;
        };
        if (seconds > 0) {
            blocks.put(provider.name(), new Block(Instant.now().plusSeconds(seconds), attempt));
        }
    }

    private CallFailed fail(AiProperties.Resolved provider, Reason reason, Long retryAfter, String detail) {
        log.warn("AI provider {} ({}) failed: {} - {}", provider.name(), provider.model(), reason, detail);
        return new CallFailed(new Attempt(provider.name(), provider.model(), reason, retryAfter));
    }

    private static long secondsLeft(Instant until, Instant now) {
        return Math.max(1, (long) Math.ceil(Duration.between(now, until).toMillis() / 1000.0));
    }

    private static String clip(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "..." : oneLine;
    }
}
