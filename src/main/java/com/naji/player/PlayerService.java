package com.naji.player;

public interface PlayerService {
    void registerPlayer(PlayerRequest playerRequest);

    void updatePlayer(PlayerRequest playerRequest, String token);

    void resetPassword(ResetPasswordRequest resetRequest);

    Player getPlayerByIdOrThrowException(Long id);

    void deletePlayer(Long id, String token);

    void joinRoom(String passCode, String token);
}
