package com.naji.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room>findByPassCode(String passCode);
    @Query(value = "SELECT nextval('passcode_sequence')", nativeQuery = true)
    Long getNextPassCodeNumber();
}
