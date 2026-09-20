package com.naji.room;

import com.naji.leaderboard.Leaderboard;
import com.naji.player.Player;
import com.naji.round.Round;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_seq")
    @SequenceGenerator(name = "room_seq", sequenceName = "room_seq", allocationSize = 50)
    private Long id;

    @Column(name = "pass_code", unique = true, nullable = false)
    private String passCode;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_players",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    @Builder.Default
    private List<Player> players = new ArrayList<>();

    private Boolean isActive = true;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Round> rounds = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "admin_id")
    private Player admin;

    private Integer currentRound = 0;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "leaderboard_id")
    private Leaderboard leaderboard;
}