package com.naji.player;

import com.naji.exception.exceptions.TokenNotValidException;
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

    @PostMapping("/register")
    public ApiResponse<String> registerPlayer(@Validated(OnCreate.class) @RequestBody PlayerRequest playerRequest) {
        playerService.registerPlayer(playerRequest);
        return new ApiResponse<>("email verification required, check your email: " + playerRequest.getEmail(), HttpStatus.CREATED);
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
       try{
           String token = jwtUtils.getTokenFromHeader(authHeader);
          playerService.updatePlayer(playerRequest, token);
           return new ApiResponse<>("email verification is required, check your email", HttpStatus.OK);
       }
       catch(TokenNotValidException ex) {
           return new ApiResponse<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
       }
    }

    @PutMapping("/reset-password")
    public ApiResponse<?> resetPassword(@Validated(OnCreate.class) @RequestBody  ResetPasswordRequest resetRequest){
        playerService.resetPassword(resetRequest);
        return new ApiResponse<>("email verification required, check your email", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ApiResponse<Player> getPlayerById(@PathVariable Long id) {
        Player player = playerService.getPlayerByIdOrThrowException(id);
        return new ApiResponse<>(player, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deletePlayer(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        playerService.deletePlayer(id, token);
        return new ApiResponse<>("player deleted successfully", HttpStatus.OK);
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody @Validated(OnCreate.class) Request playerRequest) {
        logger.info("reaches the backend");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(playerRequest.getUserName(), playerRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtils.generateToken(playerRequest.getUserName());
            return new ApiResponse<>(token, HttpStatus.OK);

        } catch (Exception e) {
            return new ApiResponse<>(e.getLocalizedMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
}

