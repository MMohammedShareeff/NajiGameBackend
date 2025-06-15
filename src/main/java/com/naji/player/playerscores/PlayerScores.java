package com.naji.player.playerscores;

import com.naji.leaderboard.Leaderboard;
import com.naji.player.Player;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player_scores")
public class PlayerScores {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "leaderboard_id")
    private Leaderboard leaderboard;

    @ElementCollection
    private List<Integer> scores;
}
