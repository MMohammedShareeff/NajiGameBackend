package com.naji.game;

import java.util.List;

public record RoundSubmissionsMessage(int round, List<SubmissionInfo> submissions) {

    public record SubmissionInfo(String playerName, long millisIntoRound) {
    }
}

