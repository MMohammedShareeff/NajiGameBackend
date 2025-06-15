package com.naji.room;

import com.naji.player.Player;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public interface RoomService {
    List<Player> getPlayersInRoom(String passCode);
     Room createRoom(String token);
     Room addPlayerToRoom(String passCode, String userName);
     void KickPlayerFromRoom(String passCode, Long requesterId, String playerName);
     Room getRoomByPassCodeOrThrowException(String passCode);

}
