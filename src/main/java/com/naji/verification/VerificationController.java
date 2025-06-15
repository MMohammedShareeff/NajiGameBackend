package com.naji.verification;

import com.naji.exception.exceptions.TokenNotValidException;
import com.naji.response.ApiResponse;
import com.naji.security.jwt.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/verification")
public class VerificationController {
    private final VerificationService verificationService;
    private final JWTUtils jwtUtils;

    @PostMapping("/verify-email")
    public ApiResponse<String> verifyEmail(@RequestParam String email, @RequestParam String verificationCode,
                                           HttpServletRequest request,
                                           @RequestParam boolean isUpdate,
                                           @RequestParam boolean isPassReset){
        boolean isVerified;
        if(!isUpdate && !isPassReset){
            isVerified = verificationService.verifyAndSaveUser(email, verificationCode);
        }
        else if(isUpdate && isPassReset) {
            return new ApiResponse<>("server can't understand the request", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else if (isUpdate) {
            String authHeader = request.getHeader("Authorization");
            String token;
            try{
                token = jwtUtils.getTokenFromHeader(authHeader);
            }
            catch (TokenNotValidException ex) {
                return new ApiResponse<>(ex.getMessage(), HttpStatus.FORBIDDEN);
            }
            isVerified = verificationService.updateAndVerify(email, verificationCode, token);
        }
        else {
            isVerified = verificationService.verifyAndSavePassword(email, verificationCode);
        }

        return isVerified
                ? new ApiResponse<>("Email verified successfully", HttpStatus.OK)
                : new ApiResponse<>("Failed to verify email, your code is either expired or wrong", HttpStatus.BAD_REQUEST);
    }
}
