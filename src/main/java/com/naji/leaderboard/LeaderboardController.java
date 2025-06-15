package com.naji.leaderboard;

import com.naji.response.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RequestMapping("leaderboard")
@RestController
public class LeaderboardController {

    private LeaderboardService leaderboardService;

    @GetMapping("/get-by-room-pasCode")
    public ApiResponse<String> getLeaderboardByRoomPassCode(@RequestParam String passCode){
        String leaderboard = leaderboardService.getLeaderboardByRoomPassCode(passCode);
        return new ApiResponse<>(leaderboard, HttpStatus.OK);
    }
}
