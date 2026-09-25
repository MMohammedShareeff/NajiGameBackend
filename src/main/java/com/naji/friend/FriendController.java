package com.naji.friend;

import com.naji.response.ApiResponse;
import com.naji.security.jwt.JWTUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final JWTUtils jwtUtils;

    @GetMapping
    public ApiResponse<List<FriendResponse>> list(@RequestHeader("Authorization") String authHeader) {
        return new ApiResponse<>(friendService.list(playerId(authHeader)), HttpStatus.OK);
    }

    @PostMapping("/add")
    public ApiResponse<String> add(@RequestParam String userName, @RequestHeader("Authorization") String authHeader) {
        return new ApiResponse<>(friendService.add(playerId(authHeader), userName), HttpStatus.OK);
    }

    @DeleteMapping("/{userName}")
    public ApiResponse<String> remove(@PathVariable String userName, @RequestHeader("Authorization") String authHeader) {
        friendService.remove(playerId(authHeader), userName);
        return new ApiResponse<>("friend removed", HttpStatus.OK);
    }

    private Long playerId(String authHeader) {
        return jwtUtils.getPlayerIdFromToken(jwtUtils.getTokenFromHeader(authHeader));
    }
}
