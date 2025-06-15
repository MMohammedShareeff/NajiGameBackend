package com.naji.player;

import lombok.Builder;

@Builder
public class PlayerMapper {

    public static Player toEntity(PlayerRequest playerRequest) {
        return Player.builder()
                .userName(playerRequest.getUserName())
                .email(playerRequest.getEmail())
                .password(playerRequest.getPassword())
                .userName(playerRequest.getUserName())
                .build();
    }

    public static PlayerResponse toResponse(Player player) {
        return PlayerResponse.builder()
                .email(player.getEmail())
                .id(player.getId())
                .userName(player.getUserName())
                .lastGameStatus(player.getLastGameStatus())
                .role(player.getRole())
                .build();
    }
}
