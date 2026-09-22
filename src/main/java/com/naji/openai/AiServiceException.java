package com.naji.openai;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AiServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        RATE_LIMIT, QUOTA_EXHAUSTED, INVALID_KEY, MODEL_NOT_FOUND, BAD_REQUEST,
        PROVIDER_DOWN, TIMEOUT, NETWORK, BAD_RESPONSE
    }

    public record Attempt(String provider, String model, Reason reason, Long retryAfterSeconds) {

        public Attempt withRetryAfter(Long seconds) {
            return new Attempt(provider, model, reason, seconds);
        }

        public String describe() {
            String wait = retryAfterSeconds == null ? "" : " (retry in ~" + formatWait(retryAfterSeconds) + ")";
            return switch (reason) {
                case RATE_LIMIT -> provider + ": usage limit reached" + wait;
                case QUOTA_EXHAUSTED -> provider + ": no credits or quota left" + wait;
                case INVALID_KEY -> provider + ": API key rejected (invalid, revoked, expired or access denied)";
                case MODEL_NOT_FOUND ->
                        provider + ": model '" + model + "' not found (it may have been removed or is no longer free)";
                case BAD_REQUEST -> provider + ": request was rejected";
                case PROVIDER_DOWN -> provider + ": service temporarily unavailable";
                case TIMEOUT -> provider + ": did not answer in time";
                case NETWORK -> provider + ": could not connect";
                case BAD_RESPONSE -> provider + ": sent an empty or unreadable answer";
            };
        }
    }

    private final transient List<Attempt> attempts;
    private final int httpStatus;

    private AiServiceException(String message, int httpStatus, List<Attempt> attempts) {
        super(message);
        this.httpStatus = httpStatus;
        this.attempts = List.copyOf(attempts);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public List<Attempt> getAttempts() {
        return attempts;
    }

    public String getUserMessage() {
        return getMessage();
    }

    static AiServiceException notConfigured() {
        return new AiServiceException(
                "No AI provider is configured. Add a free API key to your .env "
                        + "(GROQ_API_KEY from console.groq.com, or GEMINI_API_KEY from aistudio.google.com) "
                        + "and restart the app.",
                503, List.of());
    }

    static AiServiceException allFailed(List<Attempt> attempts) {
        boolean limited = !attempts.isEmpty() && attempts.stream()
                .allMatch(a -> a.reason() == Reason.RATE_LIMIT || a.reason() == Reason.QUOTA_EXHAUSTED);

        String headline;
        if (limited) {
            Long wait = attempts.stream().map(Attempt::retryAfterSeconds).filter(Objects::nonNull)
                    .min(Long::compare).orElse(null);
            headline = wait != null
                    ? "AI usage limit reached. Please try again in ~" + formatWait(wait) + "."
                    : "AI usage limit reached. Please wait a moment and try again.";
        } else {
            headline = "The AI service is unavailable right now.";
        }
        String details = attempts.stream().map(Attempt::describe).collect(Collectors.joining("; "));
        return new AiServiceException(headline + " [" + details + "]", limited ? 429 : 503, attempts);
    }

    static String formatWait(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        if (seconds < 3600) {
            return (seconds + 59) / 60 + " min";
        }
        return (seconds + 3599) / 3600 + " h";
    }
}
