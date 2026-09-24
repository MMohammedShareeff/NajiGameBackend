package com.naji.invite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.UnauthorizedAccessException;
import com.naji.player.Player;
import com.naji.player.PlayerServiceImpl;
import com.naji.room.Room;
import com.naji.room.RoomRepository;
import com.naji.room.RoomServiceImpl;
import com.naji.websocket.WebSocketController;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class InviteService {

    private static final int INVITE_TTL_MINUTES = 10;
    private static final int MAX_ROOM_SIZE = 5;
    private static final String INVITE_KEY = "invite:";
    private static final String RECIPIENT_KEY = "invitesFor:";

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;
    private final PlayerServiceImpl playerService;
    private final RoomServiceImpl roomService;
    private final RoomRepository roomRepository;
    private final WebSocketController socketController;

    public InviteService(
            @Qualifier("verificationCodeRedisTemplate") RedisTemplate<String, String> redis,
            ObjectMapper objectMapper,
            PlayerServiceImpl playerService,
            RoomServiceImpl roomService,
            RoomRepository roomRepository,
            WebSocketController socketController) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.playerService = playerService;
        this.roomService = roomService;
        this.roomRepository = roomRepository;
        this.socketController = socketController;
    }

    @Transactional
    public String send(String passCode, Long senderId, String toUserName) {
        Player sender = playerService.getPlayerByIdOrThrowException(senderId);
        if (PlayerServiceImpl.isGuest(sender)) {
            throw new UnauthorizedAccessException("Guests can't send invites. Create an account to invite friends.");
        }

        Room room = roomService.getRoomByPassCodeOrThrowException(passCode);
        if (!isMember(room, senderId)) {
            throw new UnauthorizedAccessException(ExceptionsMessages.getUnauthorizedMessage());
        }
        if (room.getPlayers().size() >= MAX_ROOM_SIZE) {
            throw new IllegalStateException("The room is full.");
        }

        Player recipient = playerService.getPlayerByUserNameOrThrowException(toUserName == null ? "" : toUserName.trim());
        if (recipient.getId().equals(senderId)) {
            throw new IllegalStateException("You can't invite yourself.");
        }
        if (PlayerServiceImpl.isGuest(recipient)) {
            throw new IllegalStateException("You can only invite registered players.");
        }
        if (isMember(room, recipient.getId())) {
            throw new IllegalStateException(recipient.getUserName() + " is already in this room.");
        }

        boolean alreadyInvited = pendingFor(recipient.getId()).stream()
                .anyMatch(invite -> invite.passCode().equals(passCode) && invite.fromId().equals(senderId));
        if (alreadyInvited) {
            throw new IllegalStateException("You already invited " + recipient.getUserName() + " to this room.");
        }

        StoredInvite invite = new StoredInvite(
                UUID.randomUUID().toString(),
                sender.getUserName(),
                senderId,
                recipient.getId(),
                recipient.getUserName(),
                passCode,
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(INVITE_TTL_MINUTES)
        );
        store(invite);
        socketController.sendInvite(recipient.getUserName(), toResponse(invite));

        return "Invite sent to " + recipient.getUserName();
    }

    public List<InviteResponse> mine(Long playerId) {
        return pendingFor(playerId).stream()
                .sorted(Comparator.comparingLong(StoredInvite::expiresAtMillis).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public String accept(String inviteId, Long playerId) {
        StoredInvite invite = findOwnedInvite(inviteId, playerId);
        Player player = playerService.getPlayerByIdOrThrowException(playerId);

        String currentPassCode = player.getCurrentGamePassCode();
        if (currentPassCode != null && !currentPassCode.equals(invite.passCode())) {
            boolean stillInOtherRoom = roomRepository.findByPassCode(currentPassCode)
                    .map(currentRoom -> isMember(currentRoom, playerId))
                    .orElse(false);
            if (stillInOtherRoom) {
                throw new IllegalStateException("You're already in another room. Leave it first.");
            }
        }

        Room room = roomService.getRoomByPassCodeOrThrowException(invite.passCode());
        if (!isMember(room, playerId)) {
            try {
                roomService.addPlayerToRoom(invite.passCode(), player.getUserName());
            } catch (RuntimeException ex) {
                throw new IllegalStateException(ex.getMessage());
            }
        }

        remove(invite);
        return invite.passCode();
    }

    public void decline(String inviteId, Long playerId) {
        remove(findOwnedInvite(inviteId, playerId));
    }

    private StoredInvite findOwnedInvite(String inviteId, Long playerId) {
        StoredInvite invite = read(inviteId);
        if (invite == null) {
            throw new IllegalStateException("This invite has expired or was already used.");
        }
        if (!invite.toId().equals(playerId)) {
            throw new UnauthorizedAccessException(ExceptionsMessages.getUnauthorizedMessage());
        }
        return invite;
    }

    private boolean isMember(Room room, Long playerId) {
        return room.getPlayers().stream().anyMatch(player -> player.getId().equals(playerId));
    }

    private InviteResponse toResponse(StoredInvite invite) {
        long secondsLeft = Math.max(0, (invite.expiresAtMillis() - System.currentTimeMillis()) / 1000);
        return new InviteResponse(invite.id(), invite.from(), invite.passCode(), secondsLeft);
    }

    private void store(StoredInvite invite) {
        try {
            redis.opsForValue().set(INVITE_KEY + invite.id(), objectMapper.writeValueAsString(invite),
                    INVITE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not create the invite.");
        }
        String recipientKey = RECIPIENT_KEY + invite.toId();
        redis.opsForSet().add(recipientKey, invite.id());
        redis.expire(recipientKey, INVITE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private StoredInvite read(String inviteId) {
        String json = redis.opsForValue().get(INVITE_KEY + inviteId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, StoredInvite.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private void remove(StoredInvite invite) {
        redis.delete(INVITE_KEY + invite.id());
        redis.opsForSet().remove(RECIPIENT_KEY + invite.toId(), invite.id());
    }

    private List<StoredInvite> pendingFor(Long playerId) {
        String recipientKey = RECIPIENT_KEY + playerId;
        List<StoredInvite> pending = new ArrayList<>();
        Set<String> ids = redis.opsForSet().members(recipientKey);
        if (ids == null) {
            return pending;
        }
        for (String id : ids) {
            StoredInvite invite = read(id);
            if (invite == null) {
                redis.opsForSet().remove(recipientKey, id);
            } else {
                pending.add(invite);
            }
        }
        return pending;
    }
}
