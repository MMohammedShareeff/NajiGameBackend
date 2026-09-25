package com.naji.player;

import com.naji.exception.exceptions.TokenNotValidException;
import com.naji.redis.RedisService;
import com.naji.response.ApiResponse;
import com.naji.security.jwt.JWTUtils;
import com.naji.security.login.Request;
import com.naji.validation.OnCreate;
import com.naji.validation.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/player")
@Validated
public class PlayerController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    private final PlayerServiceImpl playerService;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;

    @PostMapping("/register")
    public ApiResponse<String> registerPlayer(@Validated(OnCreate.class) @RequestBody PlayerRequest playerRequest) {
        playerService.registerPlayer(playerRequest);
        return new ApiResponse<>("email verification required, check your email: " + playerRequest.getEmail(), HttpStatus.CREATED);
    }

    @PostMapping("/guest")
    public ApiResponse<String> createGuest(@RequestParam(required = false) String name) {
        String userName = playerService.createGuest(name);
        return new ApiResponse<>(jwtUtils.generateToken(userName), HttpStatus.CREATED);
    }

    @PostMapping("/join-room")
    public ApiResponse<?> joinRoom(@RequestParam String passcode, @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        playerService.joinRoom(passcode, token);
        return new ApiResponse<>("you joined the room successfully", HttpStatus.OK);
    }

    @PutMapping("/update")
    public ApiResponse<String> updatePlayer(@RequestBody @Validated(OnUpdate.class) PlayerRequest playerRequest,
                                            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = jwtUtils.getTokenFromHeader(authHeader);
            playerService.updatePlayer(playerRequest, token);
            return new ApiResponse<>("confirmation code sent to your current email", HttpStatus.OK);
        } catch (TokenNotValidException ex) {
            return new ApiResponse<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/reset-password")
    public ApiResponse<?> resetPassword(@Validated(OnCreate.class) @RequestBody ResetPasswordRequest resetRequest) {
        playerService.resetPassword(resetRequest);
        return new ApiResponse<>("email verification required, check your email", HttpStatus.OK);
    }

    @GetMapping("/me")
    public ApiResponse<?> getMyProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        if (token == null || !jwtUtils.validateJwtToken(token)) {
            return new ApiResponse<>("your token is either expired or with wrong format", HttpStatus.UNAUTHORIZED);
        }

        Player player = playerService.getPlayerByIdOrThrowException(jwtUtils.getPlayerIdFromToken(token));
        return new ApiResponse<>(PlayerMapper.toProfile(player), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getPlayerById(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        if (token == null || !jwtUtils.validateJwtToken(token)) {
            return new ApiResponse<>("your token is either expired or with wrong format", HttpStatus.UNAUTHORIZED);
        }
        if (!jwtUtils.getPlayerIdFromToken(token).equals(id)) {
            return new ApiResponse<>("you can only view your own account", HttpStatus.FORBIDDEN);
        }

        Player player = playerService.getPlayerByIdOrThrowException(id);
        return new ApiResponse<>(PlayerMapper.toResponse(player), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deletePlayer(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        playerService.deletePlayer(id, token);
        return new ApiResponse<>("player deleted successfully", HttpStatus.OK);
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody @Validated(OnCreate.class) Request playerRequest) {
        if (redisService.isLoginLocked(playerRequest.getUserName())) {
            return new ApiResponse<>("Too many failed sign-in attempts. Try again in a few minutes.", HttpStatus.TOO_MANY_REQUESTS);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(playerRequest.getUserName(), playerRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtils.generateToken(playerRequest.getUserName());
            redisService.clearLoginFailures(playerRequest.getUserName());
            return new ApiResponse<>(token, HttpStatus.OK);

        } catch (Exception e) {
            redisService.recordLoginFailure(playerRequest.getUserName());
            return new ApiResponse<>(e.getLocalizedMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
}

