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

    @PostMapping("/start")
    public ApiResponse<String> startGame(@RequestParam String passCode, @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        gameService.startGame(passCode, token);
        return new ApiResponse<>("game started successfully", HttpStatus.OK);
    }
}
