package com.naji.room;

import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.exception.exceptions.UnauthorizedAccessException;
import com.naji.player.Player;
import com.naji.player.PlayerServiceImpl;
import com.naji.security.jwt.JWTUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final JWTUtils jwtUtils;
    private final PlayerServiceImpl playerServiceImpl;

    private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);


    @Transactional
    @Override
    public List<Player> getPlayersInRoom(String passCode) {
        Room room = getRoomByPassCodeOrThrowException(passCode);
        return room.getPlayers();
    }

    @Transactional
    @Override
    public Room createRoom(String token) {
        Long creatorId = jwtUtils.getPlayerIdFromToken(token);
        logger.debug("id extracted from token is: " + creatorId);
        Player creator = playerServiceImpl.getPlayerByIdOrThrowException(creatorId);

        creator.setRole("ROOM_ADMIN");

        Room room = Room.builder()
                .passCode(generatePassCode())
                .players(new ArrayList<>() {{
                    add(creator);
                }})
                .admin(creator)
                .isActive(true)
                .build();

        return roomRepository.save(room);
    }

    @Transactional
    @Override
    public Room addPlayerToRoom(String passCode, String userName) {
        Room room = getRoomByPassCodeOrThrowException(passCode);
        Player player = playerServiceImpl.getPlayerByUserNameOrThrowException(userName);

        if (room.getPlayers().size() >= 5)
            throw new RuntimeException("Room is full. Room capacity is at most 5");

        logger.debug(String.format("room players before adding: %s ", room.getPlayers().size()));
        room.getPlayers().add(player);
        logger.debug(String.format("room players before adding: %s ", room.getPlayers().size()));
        player.setCurrentGamePassCode(passCode);
        return roomRepository.save(room);
    }

    @Transactional
    @Override
    public void KickPlayerFromRoom(String passCode, Long requesterId, String playerName) {
        Room room = getRoomByPassCodeOrThrowException(passCode);

        if (!room.getAdmin().getId().equals(requesterId))
            throw new UnauthorizedAccessException(ExceptionsMessages.getUnauthorizedMessage());

        Player player = playerServiceImpl.getPlayerByUserNameOrThrowException(playerName);

        room.getPlayers().removeIf(player1 -> player.getUserName().equals(playerName));
        roomRepository.save(room);
    }

    @Override
    public Room getRoomByPassCodeOrThrowException(String passCode) {
        return roomRepository.findByPassCode(passCode)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Room.class)
                        )
                );
    }

    private String generatePassCode() {
        Long passCodeNumber = roomRepository.getNextPassCodeNumber();
        String passCodeString = "Room-" + RandomStringUtils.randomAlphabetic(4);

        return passCodeString + passCodeNumber;
    }
}
