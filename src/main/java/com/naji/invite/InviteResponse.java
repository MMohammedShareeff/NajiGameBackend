package com.naji.invite;

public record InviteResponse(String id, String from, String passCode, long secondsLeft) {
}
