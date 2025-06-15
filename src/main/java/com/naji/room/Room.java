package com.naji.room;

import com.naji.leaderboard.Leaderboard;
import com.naji.player.Player;
import com.naji.round.Round;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String passCode;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Player> players;

    private Boolean isActive;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Round> rounds;

    @OneToOne
    private Player admin;
    private Integer currentRound;

    @OneToOne(cascade = CascadeType.ALL )
    private Leaderboard leaderboard;
}
