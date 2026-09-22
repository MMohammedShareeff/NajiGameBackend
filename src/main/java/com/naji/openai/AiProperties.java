package com.naji.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private static final Map<String, String[]> BUILT_IN = Map.of(
            "groq", new String[]{"https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile"},
            "gemini", new String[]{"https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-2.5-flash"},
            "openrouter", new String[]{"https://openrouter.ai/api/v1/chat/completions", "nvidia/nemotron-3-ultra-550b-a55b:free"},
            "openai", new String[]{"https://api.openai.com/v1/chat/completions", "gpt-4o"}
    );

    private int timeoutSeconds = 30;
    private List<String> providerOrder = new ArrayList<>();
    private Map<String, Provider> providers = new LinkedHashMap<>();

    public static class Provider {
        private String url;
        private String model;
        private String apiKey;
        private boolean apiKeyRequired = true;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isApiKeyRequired() {
            return apiKeyRequired;
        }

        public void setApiKeyRequired(boolean apiKeyRequired) {
            this.apiKeyRequired = apiKeyRequired;
        }
    }

    public record Resolved(String name, String url, String model, String apiKey) {
    }

    public List<Resolved> activeChain() {
        Map<String, Provider> byName = new LinkedHashMap<>();
        providers.forEach((k, v) -> byName.put(k.trim().toLowerCase(Locale.ROOT), v));

        List<String> names = providerOrder == null || providerOrder.isEmpty()
                ? new ArrayList<>(byName.keySet())
                : providerOrder;

        List<Resolved> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : names) {
            String name = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            Provider p = byName.get(name);
            if (name.isEmpty() || p == null || !seen.add(name)) {
                continue;
            }
            String key = p.getApiKey() == null ? "" : p.getApiKey().trim();
            if (p.isApiKeyRequired() && key.isEmpty()) {
                continue;
            }
            String[] defaults = BUILT_IN.getOrDefault(name, new String[]{"", ""});
            String url = firstNonBlank(p.getUrl(), defaults[0]);
            String model = firstNonBlank(p.getModel(), defaults[1]);
            if (url.isEmpty() || model.isEmpty()) {
                continue;
            }
            chain.add(new Resolved(name, url, model, key));
        }
        return chain;
    }

    private static String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public List<String> getProviderOrder() {
        return providerOrder;
    }

    public void setProviderOrder(List<String> providerOrder) {
        this.providerOrder = providerOrder;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }
}
