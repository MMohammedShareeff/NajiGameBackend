package com.naji.round;

public record RoundStartMessage(int round, int totalRounds, int seconds, String scenario) {
}
