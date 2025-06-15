package com.naji.dashboard;

import com.naji.player.Player;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "dashboard")
public class Dashboard {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "player_id")
    private Player player;

    private Integer totalGamesPlayed;
    private Integer totalGamesWon;
    private Integer totalGamesLost;
    private Integer totalGamesDrawn;
    private Double highestScore;
    private Double averageScorePerRound;
    private Integer winStreak;
}
