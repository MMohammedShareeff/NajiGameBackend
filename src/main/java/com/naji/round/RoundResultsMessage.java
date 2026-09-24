package com.naji.round;

import java.util.List;

public record RoundResultsMessage(int round, List<PlayerRoundResult> results) {
}
