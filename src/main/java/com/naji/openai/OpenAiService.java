package com.naji.openai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OpenAiService {

    private final WebClient.Builder webClientBuilder;

    @Autowired
    public OpenAiService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Value("${api.key}")
    private String apikey;

    public String getResponse(String scenario, String text, String playerName) {
        WebClient webClient = webClientBuilder.build();
        String body = """
        {
        "model" : "gpt-4o",
        "messages" :
            [
                {
                    "role" : "user",
                    "content" : "I want you to act as a game, the description of the game follows : the Game basically is that there is  an imaginary scenario consisting of around 10 words, and then i have to write a 20 - 50 words describing what im going to do to survive, and you are going to rate my solution from 0 - 10, and you are going to generate what is going to happen with my solution, for example you the scenario is (you are facing an angry bear) i tell you ( i will give it honey and calm it down) and now you describe what happened to me in this situation in a mid-long paragraph deciding whether i survived or not, and finally giving my solution a rate from 0 -10. The format of the response : first display the player's response, and then display what happened to him, in the last line give me the rating and if he survived or not like this :( [Survived/Not Survived],  8/10). The scenario is :  "%s" . "%s"'s response is : "%s" ."
                }
            ]
        }
        """.formatted(scenario,playerName, text);
        Mono<String> response = webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer" + apikey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class);
        return response.block();
    }

    public String getScenario(){
        WebClient webClient = webClientBuilder.build();
        String body =
                """
                {
                    "model" : "gpt-4o",
                     "messages" : [
                        {
                            "role" : "user",
                            "content" : "There is an idea of a game, the idea is as follows: the Game basically is that there is an imaginary scenario consisting of around 10 words, and the i have to write a 20 - 50 words describing what im going to do to survive, and you are going to rate my solution from 0 - 10, and you are going to generate what is going to happen with my solution,for example the scenario is (you are facing an angry bear) i tell you ( i will give it honey and calm it down) and now you describe what happened to me in this situation in a mid-long paragraph deciding whether i survived or not, and finally giving my solution a rate from 0 -10. Now i just want you to give me a scenario to start with, it does not exceed 10 words. just the scenario and nothing other like okey or sure"
                        }
                    ]
                }
                """;
        Mono<String> response = webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apikey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class);
        return response.block();
    }
}