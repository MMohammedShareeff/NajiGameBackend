package com.naji.room;

import com.naji.player.PlayerResponse;

import java.util.List;

public record RoomPlayersMessage(String admin, List<PlayerResponse> players) {
}
