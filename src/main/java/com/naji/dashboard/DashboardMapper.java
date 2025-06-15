package com.naji.dashboard;

public class DashboardMapper {
    public static DashboardResponseDTO toResponse(Dashboard dashboard){
        return DashboardResponseDTO.builder()
                .highestScore(dashboard.getHighestScore())
                .averageScorePerRound(dashboard.getAverageScorePerRound())
                .totalGamesDrawn(dashboard.getTotalGamesDrawn())
                .totalGamesLost(dashboard.getTotalGamesLost())
                .totalGamesPlayed(dashboard.getTotalGamesPlayed())
                .totalGamesWon(dashboard.getTotalGamesWon())
                .winStreak(dashboard.getWinStreak())
                .build();
    }
}
