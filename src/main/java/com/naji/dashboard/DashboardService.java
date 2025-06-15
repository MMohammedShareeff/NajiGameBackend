package com.naji.dashboard;

import com.naji.exception.ExceptionsMessages;
import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.player.Player;
import com.naji.player.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final PlayerRepository playerRepository;

    @Transactional
    public Dashboard getDashboardForPlayer(Long playerId){
        return dashboardRepository.findByPlayerId(playerId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Dashboard.class)
                        )
                );
    }
    @Transactional
    public void updateDashboardForPlayer(Long playerId, DashboardUpdateRequest dashboardUpdateRequest) {
        Dashboard dashboard = dashboardRepository.findByPlayerId(playerId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Dashboard.class)
                        )
                );

        var gameStatus = dashboardUpdateRequest.getGameStatus();
        double gameScore = dashboardUpdateRequest.getScore();

        switch (gameStatus) {
            case WIN -> {
                dashboard.setTotalGamesWon(dashboard.getTotalGamesWon() + 1);
                dashboard.setWinStreak(dashboard.getWinStreak() + 1);
            }
            case LOSE -> {
                dashboard.setTotalGamesLost(dashboard.getTotalGamesLost() + 1);
                dashboard.setWinStreak(0);
            }
            case DRAW -> {
                dashboard.setTotalGamesDrawn(dashboard.getTotalGamesDrawn() + 1);
                dashboard.setWinStreak(0);
            }
        }

        dashboard.setTotalGamesPlayed(dashboard.getTotalGamesPlayed() + 1);
        Stream.of(gameScore)
                .filter(currentScore -> currentScore > dashboard.getHighestScore())
                .findFirst()
                .ifPresent(dashboard::setHighestScore);

        Integer cntGames = dashboard.getTotalGamesPlayed();
        double newTotal = (dashboard.getAverageScorePerRound()) * (cntGames - 1) + gameScore;
        double newAverage = newTotal / cntGames;
        dashboard.setAverageScorePerRound(newAverage);

        dashboardRepository.save(dashboard);
    }

    public void initializeDashboardStatus(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(
                        () ->  new ResourceNotFoundException(
                                ExceptionsMessages.getResourceNotFoundMessage(Dashboard.class)
                        )
                );

        Dashboard dashboard = Dashboard.builder()
                .id(playerId)
                .player(player)
                .totalGamesPlayed(0)
                .totalGamesWon(0)
                .totalGamesDrawn(0)
                .totalGamesLost(0)
                .winStreak(0)
                .highestScore(0.0)
                .averageScorePerRound(0.0)
                .build();

        player.setDashboard(dashboard);
        dashboardRepository.save(dashboard);
    }
}
