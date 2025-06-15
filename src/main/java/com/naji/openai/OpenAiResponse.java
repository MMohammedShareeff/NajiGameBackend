package com.naji.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
@Data

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiResponse {
    public String id;
    public String object;
    public Long created;
    public String model;
    public List<Choice> choices;
    public Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        public int index;
        public Message message;
        public String finish_reason;

    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        public String role;
        public String content;
        public String refusal;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        public int prompt_tokens;
        public int completion_tokens;
        public int total_tokens;
    }
}
