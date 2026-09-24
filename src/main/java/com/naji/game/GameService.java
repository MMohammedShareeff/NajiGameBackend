package com.naji.game;

import com.naji.dashboard.DashboardService;
import com.naji.dashboard.DashboardUpdateRequest;
import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.InsufficientPlayersException;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.exception.exceptions.UnauthorizedAccessException;
import com.naji.leaderboard.Leaderboard;
import com.naji.leaderboard.LeaderboardRepository;
import com.naji.leaderboard.LeaderboardService;
import com.naji.openai.AiServiceException;
import com.naji.player.Player;
import com.naji.player.playerscores.PlayerScores;
import com.naji.player.playerscores.PlayerScoresRepository;
import com.naji.room.Room;
import com.naji.room.RoomRepository;
import com.naji.round.PlayerRoundResult;
import com.naji.round.Round;
import com.naji.round.RoundRepository;
import com.naji.round.RoundResultsMessage;
import com.naji.round.RoundService;
import com.naji.round.RoundStartMessage;
import com.naji.security.jwt.JWTUtils;
import com.naji.submission.SubmissionRepository;
import com.naji.websocket.WebSocketController;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Scope("prototype")
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private static final Set<Long> RUNNING_ROOM_IDS = ConcurrentHashMap.newKeySet();
    private static final int LAST_ROUND = 5;
    private static final int RESULTS_BASE_SECONDS = 3;
    private static final int RESULTS_SECONDS_PER_PLAYER = 8;

    private final RoomRepository roomRepository;
    private final WebSocketController socketController;
    private final LeaderboardRepository leaderboardRepository;
    private final PlayerScoresRepository playerScoresRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final DashboardService dashboardService;
    private final RoundService roundService;
    private final LeaderboardService leaderboardService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JWTUtils jwtUtils;
    private final PlatformTransactionManager transactionManager;

    @Value("${game.round-seconds:90}")
    private int roundSeconds;

    private Room room;
    private ScheduledFuture<?> pendingRoundEnd;

    @Transactional
    public void startGame(String passCode, String token) {

        room = roomRepository.findByPassCode(passCode)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Room.class)
                        )
                );

        String userName = jwtUtils.getUserNameFromJwtToken(token);

        Player roomAdmin = room.getAdmin();
        boolean isAdmin = Optional.ofNullable(roomAdmin)
                .map(
                        admin -> admin.getUserName().equals(userName)
                )
                .orElse(false);

        if (!isAdmin) {
            throw new UnauthorizedAccessException(
                    ExceptionsMessages.getUnauthorizedMessage()
            );
        }

        if (room.getPlayers().size() < 2) {
            throw new InsufficientPlayersException(
                    ExceptionsMessages.getInsufficientPlayersMessage()
            );
        }

        if (!RUNNING_ROOM_IDS.add(room.getId())) {
            throw new IllegalStateException("A game is already in progress in this room.");
        }

        try {
            Leaderboard leaderboard = Leaderboard.builder()
                    .players(new ArrayList<>(room.getPlayers()))
                    .build();
            room.setLeaderboard(leaderboard);
            leaderboardRepository.save(leaderboard);

            for (Player player : room.getPlayers()) {
                player.setLeaderboard(leaderboard);
                PlayerScores playerScores = PlayerScores.builder()
                        .player(player)
                        .leaderboard(leaderboard)
                        .scores(new ArrayList<>())
                        .build();
                playerScoresRepository.save(playerScores);
            }

            room.setIsActive(true);
            room.setCurrentRound(0);
            roomRepository.save(room);
            socketController.broadcastGameStart(room.getId(), "The game has started");

            startRound();
        } catch (RuntimeException ex) {
            RUNNING_ROOM_IDS.remove(room.getId());
            throw ex;
        }
    }

    private void startRound() {
        int currentRound = room.getCurrentRound() + 1;
        room.setCurrentRound(currentRound);

        Round round = new Round(currentRound);
        round.setRoom(room);
        roundService.startRound(round);
        socketController.broadcastRoundStarts(
                room.getId(), new RoundStartMessage(currentRound, LAST_ROUND, roundSeconds, round.getScenario()));

        Long roundId = round.getId();
        Long roomId = room.getId();

        pendingRoundEnd = scheduler.schedule(() -> runRoundEnd(roundId, roomId), roundSeconds, TimeUnit.SECONDS);
    }

    /**
     * Called by SubmissionService after every committed submission. Counts again in a fresh
     * transaction, so two players submitting at the same instant cannot both miss that they were
     * the last one. Runs on the same single-threaded executor as the timer, so cancelling the
     * pending timer here is safe from racing with it, and runRoundEnd ignores an already-ended round.
     */
    public void triggerEarlyRoundEndIfComplete(Long roundId, Long roomId) {
        scheduler.execute(() -> {
            try {
                if (!allPlayersSubmitted(roundId, roomId)) {
                    return;
                }
                if (pendingRoundEnd != null) {
                    pendingRoundEnd.cancel(false);
                }
                runRoundEnd(roundId, roomId);
            } catch (Throwable ex) {
                logger.error("Early round end check failed for room {}: {}", roomId, ex.getMessage(), ex);
            }
        });
    }

    private boolean allPlayersSubmitted(Long roundId, Long roomId) {
        return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status -> {
            int submitted = submissionRepository.countByRoundId(roundId);
            int players = roomRepository.findById(roomId)
                    .map(currentRoom -> currentRoom.getPlayers().size())
                    .orElse(Integer.MAX_VALUE);
            return submitted >= players;
        }));
    }

    private void runRoundEnd(Long roundId, Long roomId) {
        runInTransaction(roomId, () -> {
            Round freshRound = roundRepository.findById(roundId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ExceptionsMessages.getResourceNotFoundMessage(Round.class)));
            if (!Boolean.TRUE.equals(freshRound.getActive())) {
                return;
            }
            roundService.endRound(freshRound);
            processRound();
        });
    }

    /**
     * Scheduled work runs on a plain executor thread with no Hibernate session, so every step
     * re-loads the room inside its own transaction. Any failure stops the game and tells the
     * players, instead of leaving them waiting on a round that will never finish.
     */
    private void runInTransaction(Long roomId, Runnable action) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                room = roomRepository.findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Room.class)));
                action.run();
            });
        } catch (Throwable ex) {
            logger.error("Game processing failed for room {}: {}", roomId, ex.getMessage(), ex);
            stopGameAfterFailure(roomId, ex);
        }
    }

    private void processRound() {
        Round round = roundRepository.findFirstByRoomIdAndNoOfRoundOrderByIdDesc(room.getId(), room.getCurrentRound())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ExceptionsMessages.getResourceNotFoundMessage(Round.class)
                ));
        List<PlayerRoundResult> results = roundService.processSubmissions(round.getId());

        socketController.broadcastRoundResults(room.getId(), new RoundResultsMessage(room.getCurrentRound(), results));
        socketController.broadcastLeaderboardUpdate(
                room.getId(), leaderboardService.getLeaderboardByRoomPassCode(room.getPassCode()));

        roundRepository.save(round);
        roomRepository.save(room);

        long resultsSeconds = RESULTS_BASE_SECONDS + (long) results.size() * RESULTS_SECONDS_PER_PLAYER;
        Long roomId = room.getId();
        Runnable next = room.getCurrentRound().equals(LAST_ROUND) ? this::endGame : this::startRound;
        scheduler.schedule(() -> runInTransaction(roomId, next), resultsSeconds, TimeUnit.SECONDS);
    }

    private void endGame() {
        String finalLeaderboard = leaderboardService.getLeaderboardByRoomPassCode(room.getPassCode());
        updateDashboard(room.getLeaderboard());
        room.setIsActive(false);
        roomRepository.save(room);
        RUNNING_ROOM_IDS.remove(room.getId());
        socketController.broadcastGameEnds(room.getId(), finalLeaderboard);
    }

    private void stopGameAfterFailure(Long roomId, Throwable failure) {
        RUNNING_ROOM_IDS.remove(roomId);
        String reason = failure instanceof AiServiceException aiFailure
                ? aiFailure.getUserMessage()
                : "Something went wrong while judging the round.";

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    roomRepository.findById(roomId).ifPresent(stoppedRoom -> {
                        stoppedRoom.setIsActive(false);
                        roomRepository.save(stoppedRoom);
                    }));
        } catch (Throwable ex) {
            logger.error("Could not mark room {} inactive after a failure: {}", roomId, ex.getMessage(), ex);
        }

        socketController.broadcastUpdate(roomId, "The game was stopped. " + reason);
    }

    private void updateDashboard(Leaderboard finalLeaderBoard) {
        Map<Player, Integer> playerScoresMap = new HashMap<>();
        for (Player player : room.getPlayers()) {
            PlayerScores playerScores = playerScoresRepository.findByPlayerIdAndLeaderboardId(player.getId(), finalLeaderBoard.getId())
                    .orElseThrow(() -> new RuntimeException("Player scores not found for leaderboard"));

            int totalScore = playerScores.getScores().stream().mapToInt(Integer::intValue).sum();
            playerScoresMap.put(player, totalScore);
        }

        int maxScore = playerScoresMap.values().stream().mapToInt(Integer::intValue).max()
                .orElseThrow(() -> new RuntimeException("Unable to determine max score"));

        List<Player> winners = playerScoresMap.entrySet().stream()
                .filter(entry -> entry.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .toList();

        for (Player player : room.getPlayers()) {
            int totalScore = playerScoresMap.get(player);
            String gameStatus;

            if (winners.size() > 1 && winners.contains(player)) {
                gameStatus = "DRAW";
            } else if (winners.size() == 1 && winners.contains(player)) {
                gameStatus = "WIN";
            } else {
                gameStatus = "LOSE";
            }

            DashboardUpdateRequest dashboardUpdateRequest = new DashboardUpdateRequest(totalScore, gameStatus);
            dashboardService.updateDashboardForPlayer(player.getId(), dashboardUpdateRequest);
        }
    }
}
