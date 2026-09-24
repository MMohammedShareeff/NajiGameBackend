package com.naji.submission;

import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.game.GameService;
import com.naji.player.Player;
import com.naji.player.PlayerServiceImpl;
import com.naji.room.Room;
import com.naji.room.RoomRepository;
import com.naji.round.Round;
import com.naji.round.RoundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final RoomRepository roomRepository;
    private final RoundRepository roundRepository;
    private final PlayerServiceImpl playerServiceImpl;
    private final GameService gameService;

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

        Round round = roundRepository.findFirstByRoomIdAndNoOfRoundOrderByIdDesc(room.getId(), room.getCurrentRound())
                .orElseThrow(() -> new IllegalArgumentException("No active round found in the room."));
        if (!Boolean.TRUE.equals(round.getActive())) {
            throw new IllegalStateException("This round has ended, answers are closed.");
        }

        Integer submissionCount = submissionRepository.countByRoundId(round.getId());
        Integer playerCount = room.getPlayers().size();

        if (submissionRepository.existsByRoundIdAndPlayerId(round.getId(), playerId)) {
            throw new IllegalStateException("You can submit only once in each round.");
        }

        if (submissionCount.equals(playerCount)) {
            return "All players already submitted";
        }

        Submission submission = SubmissionMapper.toEntity(text, round, player);
        submissionRepository.save(submission);
        round.getSubmissions().add(submission);

        Long roundId = round.getId();
        Long roomId = room.getId();
        String playerName = player.getUserName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                gameService.onSubmissionRecorded(roomId, playerName);
                gameService.triggerEarlyRoundEndIfComplete(roundId, roomId);
            }
        });

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
