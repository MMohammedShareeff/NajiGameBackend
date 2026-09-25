package com.naji.round;

import java.util.Map;

public record RoundStartMessage(int round, int totalRounds, int seconds, String scenario,
                                Map<String, String> scenarios, String theme) {
}
