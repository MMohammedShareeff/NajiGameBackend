package com.naji.submission;

import com.naji.player.Player;
import com.naji.round.Round;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "submission")

public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    private String text;
    @Builder.Default
    private Boolean processed = false;
}
