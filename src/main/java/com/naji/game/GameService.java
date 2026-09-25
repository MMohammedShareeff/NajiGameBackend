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
import com.naji.openai.GameLanguage;
import com.naji.openai.ScenarioTheme;
import com.naji.openai.ScenarioThemes;
import com.naji.player.Player;
import com.naji.redis.RedisService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Service
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private static final Set<Long> RUNNING_ROOM_IDS = ConcurrentHashMap.newKeySet();
    private static final Map<Long, RoundState> ROUND_STATES = new ConcurrentHashMap<>();
    private static final Map<Long, Map<String, Long>> ROUND_SUBMISSIONS = new ConcurrentHashMap<>();
    private static final Map<Long, RoundResultsMessage> LAST_RESULTS = new ConcurrentHashMap<>();
    private static final Map<Long, String> FINAL_LEADERBOARDS = new ConcurrentHashMap<>();
    private static final Map<Long, String> STOP_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<Long, String> ROOM_LANGUAGES = new ConcurrentHashMap<>();
    private static final String PHASE_ANSWERING = "answering";
    private static final String PHASE_JUDGING = "judging";
    private static final String PHASE_RESULTS = "results";
    private static final int SCHEDULER_THREADS = 8;
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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SCHEDULER_THREADS);
    private final Map<Long, ScheduledFuture<?>> pendingRoundEnds = new ConcurrentHashMap<>();
    private final Map<Long, Object> roomLocks = new ConcurrentHashMap<>();
    private final JWTUtils jwtUtils;
    private final PlatformTransactionManager transactionManager;
    private final RedisService redisService;

    @Value("${game.round-seconds:90}")
    private int roundSeconds;

    @Value("${game.daily-limit:10}")
    private int dailyGameLimit;

    private Object lockFor(Long roomId) {
        return roomLocks.computeIfAbsent(roomId, id -> new Object());
    }

    private record RoundState(int round, String scenario, Map<String, String> scenarios, String theme, long endsAtMillis,
                               String phase) {
        RoundState withPhase(String newPhase) {
            return new RoundState(round, scenario, scenarios, theme, endsAtMillis, newPhase);
        }
    }

    private static void setPhase(Long roomId, String phase) {
        ROUND_STATES.computeIfPresent(roomId, (id, state) -> state.withPhase(phase));
    }

    private void clearRunning(Long roomId) {
        pendingRoundEnds.remove(roomId);
        RUNNING_ROOM_IDS.remove(roomId);
        ROUND_STATES.remove(roomId);
        ROUND_SUBMISSIONS.remove(roomId);
        LAST_RESULTS.remove(roomId);
        ROOM_LANGUAGES.remove(roomId);
    }

    private static List<RoundSubmissionsMessage.SubmissionInfo> submissionInfos(Long roomId) {
        return ROUND_SUBMISSIONS.getOrDefault(roomId, Map.of()).entrySet().stream()
                .map(entry -> new RoundSubmissionsMessage.SubmissionInfo(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(RoundSubmissionsMessage.SubmissionInfo::millisIntoRound))
                .toList();
    }

    public void onSubmissionRecorded(Long roomId, String playerName) {
        RoundState state = ROUND_STATES.get(roomId);
        Map<String, Long> submissions = ROUND_SUBMISSIONS.get(roomId);
        if (state == null || submissions == null || !PHASE_ANSWERING.equals(state.phase())) {
            return;
        }

        long startedAtMillis = state.endsAtMillis() - roundSeconds * 1000L;
        long millisIntoRound = Math.max(0, System.currentTimeMillis() - startedAtMillis);
        submissions.putIfAbsent(playerName, millisIntoRound);
        socketController.broadcastSubmissions(roomId, new RoundSubmissionsMessage(state.round(), submissionInfos(roomId)));
    }

    @Transactional
    public GameStateResponse getGameState(String passCode, String token) {
        Room stateRoom = roomRepository.findByPassCode(passCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ExceptionsMessages.getResourceNotFoundMessage(Room.class)));

        Long playerId = jwtUtils.getPlayerIdFromToken(token);
        boolean isMember = stateRoom.getPlayers().stream().anyMatch(player -> player.getId().equals(playerId));
        if (!isMember) {
            throw new UnauthorizedAccessException(ExceptionsMessages.getUnauthorizedMessage());
        }

        RoundState state = ROUND_STATES.get(stateRoom.getId());
        if (!RUNNING_ROOM_IDS.contains(stateRoom.getId()) || state == null) {
            return GameStateResponse.idle(FINAL_LEADERBOARDS.get(stateRoom.getId()), STOP_MESSAGES.get(stateRoom.getId()));
        }

        boolean hasSubmitted = roundRepository
                .findFirstByRoomIdAndNoOfRoundOrderByIdDesc(stateRoom.getId(), state.round())
                .map(currentRound -> Boolean.TRUE.equals(
                        submissionRepository.existsByRoundIdAndPlayerId(currentRound.getId(), playerId)))
                .orElse(false);

        String leaderboardText = null;
        if (stateRoom.getLeaderboard() != null && (state.round() > 1 || PHASE_RESULTS.equals(state.phase()))) {
            leaderboardText = leaderboardService.getLeaderboardByRoomPassCode(passCode);
        }

        long secondsLeft = Math.max(0, (state.endsAtMillis() - System.currentTimeMillis() + 999) / 1000);
        return new GameStateResponse(true, state.round(), LAST_ROUND, roundSeconds, secondsLeft,
                state.scenario(), state.scenarios(), state.theme(), state.phase(), hasSubmitted, leaderboardText, submissionInfos(stateRoom.getId()),
                PHASE_RESULTS.equals(state.phase()) ? LAST_RESULTS.get(stateRoom.getId()) : null, null, null);
    }

    public void onPlayersChanged(Long roomId) {
        if (!RUNNING_ROOM_IDS.contains(roomId)) {
            return;
        }

        try {
            Long roundId = new TransactionTemplate(transactionManager).execute(status ->
                    roomRepository.findById(roomId)
                            .filter(currentRoom -> !currentRoom.getPlayers().isEmpty())
                            .flatMap(currentRoom -> roundRepository
                                    .findFirstByRoomIdAndNoOfRoundOrderByIdDesc(roomId, currentRoom.getCurrentRound()))
                            .map(Round::getId)
                            .orElse(null));

            if (roundId == null) {
                clearRunning(roomId);
                return;
            }
            triggerEarlyRoundEndIfComplete(roundId, roomId);
        } catch (Throwable ex) {
            logger.error("Player change check failed for room {}: {}", roomId, ex.getMessage(), ex);
        }
    }

    private Room loadRoomAsAdmin(String passCode, String token) {
        Room loadedRoom = roomRepository.findByPassCode(passCode)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Room.class)
                        )
                );

        String userName = jwtUtils.getUserNameFromJwtToken(token);

        boolean isAdmin = Optional.ofNullable(loadedRoom.getAdmin())
                .map(
                        admin -> admin.getUserName().equals(userName)
                )
                .orElse(false);

        if (!isAdmin) {
            throw new UnauthorizedAccessException(
                    ExceptionsMessages.getUnauthorizedMessage()
            );
        }

        return loadedRoom;
    }

    @Transactional
    public void stopGame(String passCode, String token) {
        Room stoppedRoom = loadRoomAsAdmin(passCode, token);

        ROUND_STATES.remove(stoppedRoom.getId());
        ROUND_SUBMISSIONS.remove(stoppedRoom.getId());
        LAST_RESULTS.remove(stoppedRoom.getId());
        FINAL_LEADERBOARDS.remove(stoppedRoom.getId());
        STOP_MESSAGES.remove(stoppedRoom.getId());
        ROOM_LANGUAGES.remove(stoppedRoom.getId());
        if (!RUNNING_ROOM_IDS.remove(stoppedRoom.getId())) {
            throw new IllegalStateException("There is no game in progress in this room.");
        }

        ScheduledFuture<?> pendingEnd = pendingRoundEnds.remove(stoppedRoom.getId());
        if (pendingEnd != null) {
            pendingEnd.cancel(false);
        }

        roundRepository.findFirstByRoomIdAndNoOfRoundOrderByIdDesc(stoppedRoom.getId(), stoppedRoom.getCurrentRound())
                .ifPresent(currentRound -> currentRound.setActive(false));

        for (Player member : stoppedRoom.getPlayers()) {
            member.setCurrentGamePassCode(null);
        }
        stoppedRoom.getPlayers().clear();
        stoppedRoom.setIsActive(false);
        if (stoppedRoom.getAdmin() != null) {
            stoppedRoom.getAdmin().setRole(null);
            stoppedRoom.setAdmin(null);
        }
        roomRepository.save(stoppedRoom);

        socketController.broadcastUpdate(stoppedRoom.getId(), "The room was closed. The host ended the game.");
    }

    @Transactional
    public void setLanguage(String passCode, String token, String lang) {
        Room room = loadRoomAsAdmin(passCode, token);
        if (RUNNING_ROOM_IDS.contains(room.getId())) {
            ROOM_LANGUAGES.put(room.getId(), GameLanguage.normalize(lang));
        }
    }

    @Transactional
    public void startGame(String passCode, String token, String lang) {

        Room room = loadRoomAsAdmin(passCode, token);

        if (room.getPlayers().size() < 2) {
            throw new InsufficientPlayersException(
                    ExceptionsMessages.getInsufficientPlayersMessage()
            );
        }

        if (room.getAdmin() != null && !RUNNING_ROOM_IDS.contains(room.getId())
                && !redisService.tryConsumeDailyGame(room.getAdmin().getId(), dailyGameLimit)) {
            throw new IllegalStateException(
                    "You have reached today's limit of " + dailyGameLimit + " games. Please try again tomorrow.");
        }

        if (!RUNNING_ROOM_IDS.add(room.getId())) {
            throw new IllegalStateException("A game is already in progress in this room.");
        }
        FINAL_LEADERBOARDS.remove(room.getId());
        STOP_MESSAGES.remove(room.getId());
        ROOM_LANGUAGES.put(room.getId(), GameLanguage.normalize(lang));

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

            startRound(room);
        } catch (RuntimeException ex) {
            clearRunning(room.getId());
            throw ex;
        }
    }

    private void startRound(Room room) {
        int currentRound = room.getCurrentRound() + 1;
        room.setCurrentRound(currentRound);

        Round round = new Round(currentRound);
        round.setRoom(room);
        ScenarioTheme theme = ScenarioThemes.forRound(currentRound);
        String themeLabel = roundService.startRound(round, theme, LAST_ROUND, ROOM_LANGUAGES.getOrDefault(room.getId(), GameLanguage.ENGLISH));
        Map<String, String> scenarios = roundService.scenarioTranslations(round.getScenario());
        socketController.broadcastRoundStarts(
                room.getId(),
                new RoundStartMessage(currentRound, LAST_ROUND, roundSeconds, round.getScenario(), scenarios, themeLabel));

        Long roundId = round.getId();
        Long roomId = room.getId();

        ROUND_SUBMISSIONS.put(roomId, new ConcurrentHashMap<>());
        LAST_RESULTS.remove(roomId);
        ROUND_STATES.put(roomId, new RoundState(
                currentRound, round.getScenario(), scenarios, themeLabel,
                System.currentTimeMillis() + roundSeconds * 1000L, PHASE_ANSWERING));

        pendingRoundEnds.put(roomId, scheduler.schedule(() -> runRoundEnd(roundId, roomId), roundSeconds, TimeUnit.SECONDS));
    }


    public void triggerEarlyRoundEndIfComplete(Long roundId, Long roomId) {
        scheduler.execute(() -> {
            try {
                if (!allPlayersSubmitted(roundId, roomId)) {
                    return;
                }
                ScheduledFuture<?> pendingEnd = pendingRoundEnds.remove(roomId);
                if (pendingEnd != null) {
                    pendingEnd.cancel(false);
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
        synchronized (lockFor(roomId)) {
            if (!RUNNING_ROOM_IDS.contains(roomId)) {
                return;
            }

            runInTransaction(roomId, room -> {
                Round freshRound = roundRepository.findById(roundId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Round.class)));
                if (!Boolean.TRUE.equals(freshRound.getActive())) {
                    return;
                }
                setPhase(roomId, PHASE_JUDGING);
                roundService.endRound(freshRound);
                processRound(room);
            });
        }
    }


    private void runInTransaction(Long roomId, Consumer<Room> action) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Room room = roomRepository.findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Room.class)));
                action.accept(room);
            });
        } catch (Throwable ex) {
            logger.error("Game processing failed for room {}: {}", roomId, ex.getMessage(), ex);
            stopGameAfterFailure(roomId, ex);
        }
    }

    private void processRound(Room room) {
        Round round = roundRepository.findFirstByRoomIdAndNoOfRoundOrderByIdDesc(room.getId(), room.getCurrentRound())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ExceptionsMessages.getResourceNotFoundMessage(Round.class)
                ));
        List<PlayerRoundResult> results = roundService.processSubmissions(round.getId(), ROOM_LANGUAGES.getOrDefault(room.getId(), GameLanguage.ENGLISH));

        if (!RUNNING_ROOM_IDS.contains(room.getId())) {
            return;
        }

        RoundResultsMessage resultsMessage = new RoundResultsMessage(room.getCurrentRound(), results);
        LAST_RESULTS.put(room.getId(), resultsMessage);
        socketController.broadcastRoundResults(room.getId(), resultsMessage);
        socketController.broadcastLeaderboardUpdate(
                room.getId(), leaderboardService.getLeaderboardByRoomPassCode(room.getPassCode()));

        roundRepository.save(round);
        roomRepository.save(room);
        setPhase(room.getId(), PHASE_RESULTS);

        long resultsSeconds = RESULTS_BASE_SECONDS + (long) results.size() * RESULTS_SECONDS_PER_PLAYER;
        Long roomId = room.getId();
        Consumer<Room> next = room.getCurrentRound().equals(LAST_ROUND) ? this::endGame : this::startRound;
        scheduler.schedule(() -> {
            synchronized (lockFor(roomId)) {
                if (RUNNING_ROOM_IDS.contains(roomId)) {
                    runInTransaction(roomId, next);
                }
            }
        }, resultsSeconds, TimeUnit.SECONDS);
    }

    private void endGame(Room room) {
        String finalLeaderboard = leaderboardService.getLeaderboardByRoomPassCode(room.getPassCode());
        updateDashboard(room, room.getLeaderboard());
        room.setIsActive(false);
        roomRepository.save(room);
        clearRunning(room.getId());
        FINAL_LEADERBOARDS.put(room.getId(), finalLeaderboard);
        socketController.broadcastGameEnds(room.getId(), finalLeaderboard);
    }

    private void stopGameAfterFailure(Long roomId, Throwable failure) {
        clearRunning(roomId);
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

        String stopMessage = "The game was stopped. " + reason;
        STOP_MESSAGES.put(roomId, stopMessage);
        socketController.broadcastUpdate(roomId, stopMessage);
    }

    private void updateDashboard(Room room, Leaderboard finalLeaderBoard) {
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
