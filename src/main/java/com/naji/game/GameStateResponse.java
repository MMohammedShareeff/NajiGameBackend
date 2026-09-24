package com.naji.game;

import com.naji.round.RoundResultsMessage;

import java.util.List;

public record GameStateResponse(
        boolean running,
        int round,
        int totalRounds,
        int secondsTotal,
        long secondsLeft,
        String scenario,
        String phase,
        boolean hasSubmitted,
        String leaderboard,
        List<RoundSubmissionsMessage.SubmissionInfo> submissions,
        RoundResultsMessage results,
        String finalLeaderboard,
        String stopMessage
) {
    public static GameStateResponse idle(String finalLeaderboard, String stopMessage) {
        return new GameStateResponse(false, 0, 0, 0, 0, null, "idle", false, null, List.of(), null,
                finalLeaderboard, stopMessage);
    }
}
