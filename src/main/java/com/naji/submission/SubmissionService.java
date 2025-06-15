package com.naji.submission;

import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.player.Player;
import com.naji.player.PlayerServiceImpl;
import com.naji.room.Room;
import com.naji.room.RoomRepository;
import com.naji.round.Round;
import com.naji.round.RoundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final RoomRepository roomRepository;
    private final RoundRepository roundRepository;
    private final PlayerServiceImpl playerServiceImpl;

    @Transactional
    public String submit(String text, Long playerId) {
        Player player = playerServiceImpl.getPlayerByIdOrThrowException(playerId);

        String currentGamePassCode = player.getCurrentGamePassCode();
        if(Objects.isNull(currentGamePassCode))
            throw new UnsupportedOperationException("you are currently not joined to any game");

        Room room = roomRepository.findByPassCode(currentGamePassCode).get();
        if (!room.getIsActive()) {
            throw new IllegalStateException("the room is not active");
        }

        Round round = roundRepository.findById(room.getCurrentRound().longValue())
                .orElseThrow(() -> new IllegalArgumentException("No active round found in the room."));

        Integer submissionCount = submissionRepository.countByRoundId(round.getId());
        Integer playerCount = round.getPlayers().size();

        if (submissionRepository.existsByRoundIdAndPlayerId(round.getId(), playerId)) {
            throw new IllegalStateException("You can submit only once in each round.");
        }

        if (submissionCount.equals(playerCount)) {
            return "All players already submitted";
        }

        Submission submission = SubmissionMapper.toEntity(text, round, player);
        round.getSubmissions().add(submission);
        roundRepository.save(round);
        submissionRepository.save(submission);
        return text;
    }

    @Transactional
    public SubmissionDTO getSubmissionById(Long submissionId){
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Submission.class)
                        )
                );
        return SubmissionMapper.toDTO(submission);
    }

}
