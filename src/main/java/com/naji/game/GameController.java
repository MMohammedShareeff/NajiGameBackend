package com.naji.game;

import com.naji.response.ApiResponse;
import com.naji.security.jwt.JWTUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;
    private final JWTUtils jwtUtils;

    @GetMapping("/state")
    public ApiResponse<GameStateResponse> getGameState(@RequestParam String passCode, @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        return new ApiResponse<>(gameService.getGameState(passCode, token), HttpStatus.OK);
    }

    @PostMapping("/stop")
    public ApiResponse<String> stopGame(@RequestParam String passCode, @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        gameService.stopGame(passCode, token);
        return new ApiResponse<>("game stopped successfully", HttpStatus.OK);
    }

    @PostMapping("/language")
    public ApiResponse<String> setLanguage(@RequestParam String passCode,
                                           @RequestParam String lang,
                                           @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        gameService.setLanguage(passCode, token, lang);
        return new ApiResponse<>("language updated", HttpStatus.OK);
    }

    @PostMapping("/start")
    public ApiResponse<String> startGame(@RequestParam String passCode,
                                         @RequestParam(defaultValue = "en") String lang,
                                         @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        gameService.startGame(passCode, token, lang);
        return new ApiResponse<>("game started successfully", HttpStatus.OK);
    }
}
