package com.naji.player;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByUserName(String username);
    Optional<Player> findByEmail(String email);

}
