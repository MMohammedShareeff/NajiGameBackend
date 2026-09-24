package com.naji.dashboard;

import com.naji.response.ApiResponse;
import com.naji.security.jwt.JWTUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final JWTUtils jwtUtils;

    @GetMapping("get-by-id/{playerId}")
    public ApiResponse<?> getDashboardForPlayer(@PathVariable Long playerId,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = jwtUtils.getTokenFromHeader(authHeader);
        if (token == null || !jwtUtils.validateJwtToken(token)) {
            return new ApiResponse<>("your token is either expired or with wrong format", HttpStatus.UNAUTHORIZED);
        }
        if (!jwtUtils.getPlayerIdFromToken(token).equals(playerId)) {
            return new ApiResponse<>("you can only view your own dashboard", HttpStatus.FORBIDDEN);
        }

        Dashboard dashboard = dashboardService.getDashboardForPlayer(playerId);
        DashboardResponseDTO responseDashboard = DashboardMapper.toResponse(dashboard);
        return new ApiResponse<>(responseDashboard, HttpStatus.OK);
    }
}
