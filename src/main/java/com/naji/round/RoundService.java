package com.naji.round;


import com.naji.leaderboard.LeaderboardService;
import com.naji.openai.JsonResponseMapper;
import com.naji.openai.OpenAiService;
import com.naji.player.Player;
import com.naji.player.PlayerRepository;
import com.naji.player.playerscores.PlayerScores;
import com.naji.player.playerscores.PlayerScoresRepository;
import com.naji.submission.Submission;
import com.naji.submission.SubmissionDTO;
import com.naji.submission.SubmissionMapper;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RoundService {

    private final RoundRepository roundRepository;
    private final OpenAiService openAiService;
    private PlayerScoresRepository playerScoresRepository;
    private final PlayerRepository playerRepository;
    private final JsonResponseMapper jsonResponseMapper;
    private final LeaderboardService leaderboardService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @Getter
    private final Map<String, String> responses = new HashMap<>();

    public Boolean getStatus(Long roundId) {
        return roundRepository.findById(roundId)
                .map(Round::getActive)
                .orElse(false);
    }

    @Transactional
    public void startRound(Round round) {
        round.setActive(true);
        String scenario = getScenarioFromAPI();
        round.setScenario(scenario);
        roundRepository.save(round);
    }

    @Transactional
    public void endRound(Round round) {

        round.setActive(false);
        roundRepository.save(round);
//        shutdownScheduler();
    }

    public List<Pair<Integer, String>> processSubmissions(Long roundId) {
        List<Pair<Integer, String>> aiResponseForAll = new ArrayList<>();
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("round with id equals %d roundId not found", roundId)));

        List<SubmissionDTO> submissions = getAllSubmissionsInRound(roundId);
        for (SubmissionDTO submissionDTO : submissions) {
            Long playerId = submissionDTO.getPlayerId();
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("player with id equals %d not found", playerId)));

            String aiResponse = openAiService.getResponse(round.getScenario(), submissionDTO.getText(), player.getUserName());
            int score = jsonResponseMapper.extractRating(aiResponse);
            String status = jsonResponseMapper.extractStatus(aiResponse);
            aiResponseForAll.add(new Pair<>(score, status));

            Long leaderboardId = round.getLeaderboard().getId();
            leaderboardService.updateRoundScoreForAPlayer(leaderboardId, playerId, score);

            PlayerScores playerScores = player.getPlayerScores();
            playerScores.getScores().add(score);
            playerScoresRepository.save(playerScores);
        }
        return aiResponseForAll;
    }

    public List<SubmissionDTO> getAllSubmissionsInRound(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("round with id equals %d not found", roundId)));

        List<Submission> submissions = round.getSubmissions();
        List<SubmissionDTO> submissionDTOs = new ArrayList<>();

        for (Submission submission : submissions) {
            submissionDTOs.add(SubmissionMapper.toDTO(submission));
        }
        return submissionDTOs;
    }

    public String getScenarioFromAPI() {
        return openAiService.getScenario();
    }


    private void shutdownScheduler() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
