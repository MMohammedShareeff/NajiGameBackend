package com.naji.round;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
    Optional<Round> findFirstByRoomIdAndNoOfRoundOrderByIdDesc(Long roomId, Integer noOfRound);
}
