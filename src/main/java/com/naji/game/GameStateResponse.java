package com.naji.game;

import com.naji.round.RoundResultsMessage;

import java.util.List;
import java.util.Map;

public record GameStateResponse(
        boolean running,
        int round,
        int totalRounds,
        int secondsTotal,
        long secondsLeft,
        String scenario,
        Map<String, String> scenarios,
        String theme,
        String phase,
        boolean hasSubmitted,
        String leaderboard,
        List<RoundSubmissionsMessage.SubmissionInfo> submissions,
        RoundResultsMessage results,
        String finalLeaderboard,
        String stopMessage
) {
    public static GameStateResponse idle(String finalLeaderboard, String stopMessage) {
        return new GameStateResponse(false, 0, 0, 0, 0, null, Map.of(), null, "idle", false, null, List.of(), null,
                finalLeaderboard, stopMessage);
    }
}
