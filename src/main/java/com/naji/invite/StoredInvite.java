package com.naji.invite;

public record StoredInvite(
        String id,
        String from,
        Long fromId,
        Long toId,
        String toUserName,
        String passCode,
        long expiresAtMillis
) {
}
