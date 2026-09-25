package com.naji.room;

import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.exception.exceptions.UnauthorizedAccessException;
import com.naji.game.GameService;
import com.naji.player.Player;
import com.naji.player.PlayerMapper;
import com.naji.player.PlayerResponse;
import com.naji.player.PlayerServiceImpl;
import com.naji.security.jwt.JWTUtils;
import com.naji.websocket.WebSocketController;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final JWTUtils jwtUtils;
    private final PlayerServiceImpl playerServiceImpl;
    private final WebSocketController socketController;
    private final GameService gameService;

    private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);


    @Transactional
    public Room requireMember(String passCode, Long playerId) {
        Room room = getRoomByPassCodeOrThrowException(passCode);
        boolean isMember = room.getPlayers().stream().anyMatch(player -> player.getId().equals(playerId));
        if (!isMember) {
            throw new UnauthorizedAccessException(ExceptionsMessages.getUnauthorizedMessage());
        }
        return room;
    }

    @Transactional
    @Override
    public List<PlayerResponse> getPlayersInRoom(String passCode) {
        Room room = getRoomByPassCodeOrThrowException(passCode);
        return room.getPlayers().stream()
                .map(PlayerMapper::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public Room createRoom(String token) {
        Long creatorId = jwtUtils.getPlayerIdFromToken(token);
        logger.debug("id extracted from token is: " + creatorId);
        Player creator = playerServiceImpl.getPlayerByIdOrThrowException(creatorId);

        creator.setRole("ROOM_ADMIN");

        String passCode = generatePassCode();
        creator.setCurrentGamePassCode(passCode);

        Room room = Room.builder()
                .passCode(passCode)
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

        if (Boolean.FALSE.equals(room.getIsActive()) && room.getPlayers().isEmpty()) {
            throw new RuntimeException("This room has been closed");
        }

        if (room.getPlayers() == null) {
            room.setPlayers(new ArrayList<>());
        }

        if (room.getPlayers().size() >= 5) {
            throw new RuntimeException("Room is full. Room capacity is at most 5");
        }

        boolean alreadyInRoom = room.getPlayers().stream()
                .anyMatch(p -> p.getId().equals(player.getId()));

        if (alreadyInRoom) {
            throw new RuntimeException("Player is already in this room");
        }

        room.getPlayers().add(player);
        player.setCurrentGamePassCode(passCode);

        Room savedRoom = roomRepository.save(room);
        broadcastPlayersAfterCommit(savedRoom);
        return savedRoom;
    }

    public void broadcastPlayersAfterCommit(Room room) {
        Long roomId = room.getId();
        String adminName = room.getAdmin() == null ? null : room.getAdmin().getUserName();
        RoomPlayersMessage message = new RoomPlayersMessage(
                adminName,
                room.getPlayers().stream().map(PlayerMapper::toResponse).toList()
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                socketController.broadcastPlayers(roomId, message);
                gameService.onPlayersChanged(roomId);
            }
        });
    }

    @Transactional
    @Override
    public void leaveRoom(String passCode, Long playerId) {
        Room room = getRoomByPassCodeOrThrowException(passCode);

        Player leaver = room.getPlayers().stream()
                .filter(roomPlayer -> roomPlayer.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("You are not in this room."));

        room.getPlayers().remove(leaver);
        leaver.setCurrentGamePassCode(null);

        boolean leaverWasAdmin = room.getAdmin() != null && room.getAdmin().getId().equals(playerId);
        if (leaverWasAdmin) {
            leaver.setRole(null);
            if (room.getPlayers().isEmpty()) {
                room.setAdmin(null);
                room.setIsActive(false);
            } else {
                Player newAdmin = room.getPlayers().get(0);
                newAdmin.setRole("ROOM_ADMIN");
                room.setAdmin(newAdmin);
            }
        }

        Room savedRoom = roomRepository.save(room);
        broadcastPlayersAfterCommit(savedRoom);
    }

    @Transactional
    @Override
    public void KickPlayerFromRoom(String passCode, Long requesterId, String playerName) {
        Room room = getRoomByPassCodeOrThrowException(passCode);

        if (!room.getAdmin().getId().equals(requesterId))
            throw new UnauthorizedAccessException(ExceptionsMessages.getUnauthorizedMessage());

        Player player = playerServiceImpl.getPlayerByUserNameOrThrowException(playerName);

        room.getPlayers().removeIf(roomPlayer -> roomPlayer.getUserName().equals(playerName));
        player.setCurrentGamePassCode(null);
        Room savedRoom = roomRepository.save(room);
        broadcastPlayersAfterCommit(savedRoom);
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
        String passCode;
        do {
            passCode = "Room-" + RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (roomRepository.findByPassCode(passCode).isPresent());

        return passCode;
    }
}
