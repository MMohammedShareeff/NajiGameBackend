package com.naji.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JsonResponseMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractStatus(String jsonResponse) {
        try {
            OpenAiResponse response = objectMapper.readValue(jsonResponse, OpenAiResponse.class);
            String content = response.getChoices().get(0).getMessage().getContent();

            if(content.contains("Survived")){
                return "Survived";
            }
            else if (content.contains("Not Survived")){
                return "Not Survived";
            }
            else{
                return "Unknown";
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return "not found";
        }
    }

    public int extractRating(String jsonResponse) {
        try {
        OpenAiResponse response = objectMapper.readValue(jsonResponse, OpenAiResponse.class);
        String content = response.getChoices().get(0).getMessage().getContent();
        int rating = content.indexOf("Survived ") + 9;
        if(content.contains("/10") && rating >= 0){
            return Integer.valueOf(content.substring(rating, content.indexOf("/10")+ 3));
        }
        else {
            return -1;
        }
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return -1;
        }
    }
}
