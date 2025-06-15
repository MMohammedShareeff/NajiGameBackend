package com.naji.game;

import com.naji.dashboard.DashboardService;
import com.naji.dashboard.DashboardUpdateRequest;
import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.InsufficientPlayersException;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.exception.exceptions.RoomNotActiveException;
import com.naji.exception.exceptions.UnauthorizedAccessException;
import com.naji.leaderboard.Leaderboard;
import com.naji.leaderboard.LeaderboardRepository;
import com.naji.leaderboard.LeaderboardService;
import com.naji.player.Player;
import com.naji.player.playerscores.PlayerScores;
import com.naji.player.playerscores.PlayerScoresRepository;
import com.naji.room.Room;
import com.naji.room.RoomRepository;
import com.naji.round.Round;
import com.naji.round.RoundRepository;
import com.naji.round.RoundService;
import com.naji.security.jwt.JWTUtils;
import com.naji.websocket.WebSocketController;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Scope("prototype")
public class GameService {

    private final RoomRepository roomRepository;
    private final WebSocketController socketController;
    private final LeaderboardRepository leaderboardRepository;
    private final PlayerScoresRepository playerScoresRepository;
    private final RoundRepository roundRepository;
    private final DashboardService dashboardService;
    private final RoundService roundService;
    private final LeaderboardService leaderboardService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final JWTUtils jwtUtils;

    private Room room;

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
    }

    private void startRound() {
        int currentRound = room.getCurrentRound() + 1;
        room.setCurrentRound(currentRound);

        Round round = new Round(currentRound);
        roundService.startRound(round);
        socketController.broadcastRoundStarts(room.getId(), round.getScenario());

        scheduler.schedule(
                () -> {
                    roundService.endRound(round);
                    roomRepository.save(room);
                    processRound();
                }
                , 10, TimeUnit.SECONDS
        );
    }

    private void processRound() {
        Round round = room.getRounds().get(room.getCurrentRound() - 1);
        List<Pair<Integer, String>> fullAiResponse = roundService.processSubmissions(round.getId());

        String leaderboard = leaderboardService.getLeaderboardByRoomPassCode(room.getPassCode());
        socketController.broadcastLeaderboardUpdate(room.getId(), leaderboard);

        roundRepository.save(round);
        roomRepository.save(room);

        if (room.getCurrentRound().equals(5))
            endGame();
        else
            startRound();
    }

    public void endGame() {
        if (room.getIsActive().equals(false)) {
            throw new RoomNotActiveException(
                    ExceptionsMessages.getRoomNotActiveMessage(room)
            );
        }

        Leaderboard finalLeaderboard = room.getLeaderboard();
        updateDashboard(finalLeaderboard);
        socketController.broadcastGameEnds(room.getId(), finalLeaderboard);
        room.setIsActive(false);
        roomRepository.save(room);
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
