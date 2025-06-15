package com.naji.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponseDTO {
    private Integer totalGamesPlayed;
    private Integer totalGamesWon;
    private Integer totalGamesLost;
    private Integer totalGamesDrawn;
    private Double highestScore;
    private Double averageScorePerRound;
    private Integer winStreak;
}
