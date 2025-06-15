package com.naji.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByRoundId(Long roundId);

    Boolean existsByRoundIdAndPlayerId(Long roundId, Long playerId);
    Integer countByRoundId(Long roundId);
}
