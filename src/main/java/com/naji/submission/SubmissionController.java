package com.naji.submission;

import com.naji.response.ApiResponse;
import com.naji.security.jwt.JWTUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/Submission")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final JWTUtils jwtUtils;

    @PostMapping("/create")
    public ApiResponse<String> submit(@RequestParam String text, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = jwtUtils.getTokenFromHeader(authHeader);
            Long playerId = jwtUtils.getPlayerIdFromToken(token);
            String submission = submissionService.submit(text, playerId);
            if (submission.equals("all players submitted"))
                return new ApiResponse<>(submission, HttpStatus.BAD_REQUEST);
            return new ApiResponse<>(submission, HttpStatus.OK);
        } catch (Exception ex) {
            return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

    }

    @GetMapping("/by-id/{submissionId}")
    public ApiResponse<?> getSubmissionById(@PathVariable Long submissionId) {
        Optional<SubmissionDTO> submissionResponse = Optional.ofNullable(submissionService.getSubmissionById(submissionId));
        return submissionResponse.isPresent()
                ? new ApiResponse<>(null, HttpStatus.NO_CONTENT)
                : new ApiResponse<>(submissionResponse, HttpStatus.OK);
    }
}
