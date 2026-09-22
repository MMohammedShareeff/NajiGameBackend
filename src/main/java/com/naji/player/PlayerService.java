package com.naji.player;

import java.util.List;

public interface PlayerService {
    void registerPlayer(PlayerRequest playerRequest);

    List<Player> getAllPlayers();

    void updatePlayer(PlayerRequest playerRequest, String token);

    void resetPassword(ResetPasswordRequest resetRequest);

    Player getPlayerByIdOrThrowException(Long id);

    void deletePlayer(Long id, String token);

    void joinRoom(String passCode, String token);
}
