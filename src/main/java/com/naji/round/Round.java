package com.naji.round;

import com.naji.room.Room;
import com.naji.submission.Submission;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;


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

    private Boolean active;

    @OneToMany(mappedBy = "round")
    private List<Submission> submissions;

    public Round(Integer noOfRound) {
        this.noOfRound = noOfRound;
    }
}
