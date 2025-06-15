package com.naji.dashboard;

import lombok.Data;
@Data
public class DashboardUpdateRequest {
    private GameStatus gameStatus;
    private double score;

    public DashboardUpdateRequest(int totalScore, String gameStatus){
        this.score = totalScore;
        this.gameStatus = GameStatus.valueOf(gameStatus);
    }
}
