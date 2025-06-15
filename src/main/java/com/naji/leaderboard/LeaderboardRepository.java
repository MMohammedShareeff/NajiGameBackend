package com.naji.leaderboard;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface LeaderboardRepository extends CrudRepository<Leaderboard, Long> {
}
