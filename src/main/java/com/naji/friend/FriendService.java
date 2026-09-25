package com.naji.friend;

import com.naji.exception.exceptions.UnauthorizedAccessException;
import com.naji.player.Player;
import com.naji.player.PlayerServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private static final int MAX_FRIENDS = 100;

    private final JdbcTemplate jdbcTemplate;
    private final PlayerServiceImpl playerService;

    public List<FriendResponse> list(Long playerId) {
        return jdbcTemplate.query(
                """
                SELECT p.username FROM friend f
                JOIN player p ON p.id = f.friend_id
                WHERE f.player_id = ?
                ORDER BY lower(p.username)
                """,
                (row, index) -> new FriendResponse(row.getString(1)),
                playerId);
    }

    @Transactional
    public String add(Long playerId, String friendUserName) {
        Player owner = playerService.getPlayerByIdOrThrowException(playerId);
        requireRegistered(owner, "Guests can't keep a friends list. Create an account first.");

        Player friend = playerService.getPlayerByUserNameOrThrowException(friendUserName == null ? "" : friendUserName.trim());
        if (friend.getId().equals(playerId)) {
            throw new IllegalStateException("You can't add yourself.");
        }
        requireRegistered(friend, "You can only add registered players.");

        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM friend WHERE player_id = ?", Integer.class, playerId);
        if (count != null && count >= MAX_FRIENDS) {
            throw new IllegalStateException("Your friends list is full.");
        }

        int added = jdbcTemplate.update(
                "INSERT INTO friend (player_id, friend_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                playerId, friend.getId());
        if (added == 0) {
            throw new IllegalStateException(friend.getUserName() + " is already in your friends list.");
        }

        return friend.getUserName() + " was added to your friends.";
    }

    @Transactional
    public void remove(Long playerId, String friendUserName) {
        Player friend = playerService.getPlayerByUserNameOrThrowException(friendUserName == null ? "" : friendUserName.trim());
        jdbcTemplate.update("DELETE FROM friend WHERE player_id = ? AND friend_id = ?", playerId, friend.getId());
    }

    private void requireRegistered(Player player, String message) {
        if (PlayerServiceImpl.isGuest(player)) {
            if (message.startsWith("Guests")) {
                throw new UnauthorizedAccessException(message);
            }
            throw new IllegalStateException(message);
        }
    }
}
