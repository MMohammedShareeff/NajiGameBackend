package com.naji.round;

import com.naji.leaderboard.Leaderboard;
import com.naji.player.Player;
import com.naji.room.Room;
import com.naji.submission.Submission;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table (name = "round")
public class Round {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String scenario;
    private Integer noOfRound;

    @ManyToOne
    private Room room;

    @OneToMany
    private Set<Player> players;
    private Boolean active;

    @OneToOne
    private Leaderboard leaderboard;

    @OneToMany
    private List<Submission> submissions;

    public Round(Integer noOfRound) {
        this.noOfRound = noOfRound;
    }
}
