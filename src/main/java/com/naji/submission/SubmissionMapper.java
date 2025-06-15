package com.naji.submission;

import com.naji.player.Player;
import com.naji.round.Round;
import org.springframework.stereotype.Component;

@Component
public class SubmissionMapper {

    public static Submission toEntity(String text, Round round, Player player) {
        return Submission.builder()
                .round(round)
                .player(player)
                .text(text)
                .processed(false)
                .build();
    }

    public static SubmissionDTO toDTO(Submission submission) {
        return SubmissionDTO.builder()
                .id(submission.getId())
                .roundId(submission.getRound().getId())
                .playerId(submission.getPlayer().getId())
                .text(submission.getText())
                .processed(submission.getProcessed())
                .build();
    }
}
