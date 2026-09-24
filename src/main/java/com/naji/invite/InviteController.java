package com.naji.invite;

import com.naji.response.ApiResponse;
import com.naji.security.jwt.JWTUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;
    private final JWTUtils jwtUtils;

    @PostMapping("/send")
    public ApiResponse<String> send(@RequestParam String passCode, @RequestParam String userName,
                                    @RequestHeader("Authorization") String authHeader) {
        return new ApiResponse<>(inviteService.send(passCode, playerId(authHeader), userName), HttpStatus.OK);
    }

    @GetMapping("/mine")
    public ApiResponse<List<InviteResponse>> mine(@RequestHeader("Authorization") String authHeader) {
        return new ApiResponse<>(inviteService.mine(playerId(authHeader)), HttpStatus.OK);
    }

    @PostMapping("/{inviteId}/accept")
    public ApiResponse<String> accept(@PathVariable String inviteId, @RequestHeader("Authorization") String authHeader) {
        return new ApiResponse<>(inviteService.accept(inviteId, playerId(authHeader)), HttpStatus.OK);
    }

    @PostMapping("/{inviteId}/decline")
    public ApiResponse<String> decline(@PathVariable String inviteId, @RequestHeader("Authorization") String authHeader) {
        inviteService.decline(inviteId, playerId(authHeader));
        return new ApiResponse<>("invite declined", HttpStatus.OK);
    }

    private Long playerId(String authHeader) {
        return jwtUtils.getPlayerIdFromToken(jwtUtils.getTokenFromHeader(authHeader));
    }
}
