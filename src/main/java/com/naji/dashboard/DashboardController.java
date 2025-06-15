package com.naji.dashboard;

import com.naji.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("get-by-id/{playerId}")
    public ApiResponse<DashboardResponseDTO> getDashboardForPlayer(@PathVariable Long playerId) {
        Dashboard dashboard = dashboardService.getDashboardForPlayer(playerId);
        DashboardResponseDTO responseDashboard = DashboardMapper.toResponse(dashboard);
        return new ApiResponse<>(responseDashboard, HttpStatus.OK);
    }
}
