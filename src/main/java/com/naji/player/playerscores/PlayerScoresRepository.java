package com.naji.player.playerscores;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerScoresRepository extends JpaRepository<PlayerScores, Long> {
    Optional<PlayerScores> findByPlayerIdAndLeaderboardId(Long playerId, Long leaderBoardId);

}
