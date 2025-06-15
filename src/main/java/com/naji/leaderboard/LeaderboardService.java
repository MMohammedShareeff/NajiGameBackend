package com.naji.leaderboard;

import com.naji.player.Player;
import com.naji.player.playerscores.PlayerScores;
import com.naji.player.playerscores.PlayerScoresRepository;
import com.naji.room.Room;
import com.naji.room.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final RoomRepository roomRepository;
    private final PlayerScoresRepository playerScoresRepository;

    public String getLeaderboardByRoomPassCode(String passCode) {
        Room room = roomRepository.findByPassCode(passCode)
                .orElseThrow(() -> new IllegalArgumentException("no room with the given id"));

        Leaderboard leaderboard = room.getLeaderboard();
        StringBuilder currentLeaderboard = new StringBuilder("Leaderboard:\n");

        for (Player player : leaderboard.getPlayers()) {
            PlayerScores playerScores = playerScoresRepository.findByPlayerIdAndLeaderboardId(player.getId(), leaderboard.getId())
                    .orElseThrow(() -> new RuntimeException("Player scores not found for leaderboard"));

            int totalScore = playerScores.getScores().stream().mapToInt(Integer::intValue).sum();

            currentLeaderboard.append("Player: ")
                    .append(player.getUserName())
                    .append('\n')
                    .append("rounds scores: ")
                    .append(playerScores)
                    .append('\n')
                    .append(" - Total Score: ")
                    .append(totalScore)
                    .append("\n");
        }
        return currentLeaderboard.toString();
    }

    @Transactional
    public void updateRoundScoreForAPlayer(Long leaderboardId, Long playerId, int roundScore) {
        Leaderboard leaderboard = leaderboardRepository.findById(leaderboardId)
                .orElseThrow(() -> new RuntimeException("Leaderboard not found"));

        PlayerScores playerScores = playerScoresRepository.findByPlayerIdAndLeaderboardId(playerId, leaderboardId)
                .orElseThrow(() -> new RuntimeException("Player not found in leaderboard"));

        playerScores.getScores().add(roundScore);
        playerScoresRepository.save(playerScores);
    }

}
