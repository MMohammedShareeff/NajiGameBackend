    package com.naji.player;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import com.naji.dashboard.Dashboard;
    import com.naji.dashboard.GameStatus;
    import com.naji.leaderboard.Leaderboard;
    import com.naji.player.playerscores.PlayerScores;
    import com.naji.round.Round;
    import com.naji.submission.Submission;
    import jakarta.persistence.*;
    import lombok.*;

    import java.util.List;

    @Getter
    @Setter
    @RequiredArgsConstructor
    @AllArgsConstructor
    @Entity
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Table(
            name = "player",
            indexes = {
                    @Index(name = "user_name_idx", columnList = "userName"),
                    @Index(name = "email_idx", columnList = "email")
            }
    )
    public class Player{

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @Column(unique = true, name = "username", table = "player")
        private String userName;
        private String password;

        @Column(unique = true)
        private String email;
        private String role;

        @OneToOne(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private Dashboard dashboard;

        @ManyToOne
        private Round round;

        @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<Submission> submissions;

        @ManyToOne
        @JoinColumn(name = "leaderboard_id")
        private Leaderboard leaderboard;

        @OneToOne(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private PlayerScores playerScores;

        private  String currentGamePassCode;

        private GameStatus lastGameStatus;
    }
