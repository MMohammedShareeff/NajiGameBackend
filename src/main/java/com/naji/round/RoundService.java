package com.naji.round;


import com.naji.leaderboard.LeaderboardService;
import com.naji.openai.JsonResponseMapper;
import com.naji.openai.OpenAiService;
import com.naji.player.Player;
import com.naji.player.PlayerRepository;
import com.naji.submission.Submission;
import com.naji.submission.SubmissionDTO;
import com.naji.submission.SubmissionMapper;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RoundService {

    private static final int SURVIVAL_THRESHOLD = 5;
    private static final String SPEECHLESS_COMMENTARY = "Even the comedian was left speechless by that one.";
    private static final String NO_ANSWER_COMMENTARY =
            "No plan, no answer, no survival. The danger got a free lunch while this contestant was still thinking.";

    private final RoundRepository roundRepository;
    private final OpenAiService openAiService;
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

    public List<PlayerRoundResult> processSubmissions(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("round with id equals %d roundId not found", roundId)));

        Long leaderboardId = round.getRoom().getLeaderboard().getId();
        List<PlayerRoundResult> results = new ArrayList<>();
        Set<Long> answeredPlayerIds = new HashSet<>();

        for (SubmissionDTO submissionDTO : getAllSubmissionsInRound(roundId)) {
            Long playerId = submissionDTO.getPlayerId();
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("player with id equals %d not found", playerId)));

            String aiResponse = openAiService.getResponse(round.getScenario(), submissionDTO.getText(), player.getUserName());
            int score = Math.max(0, jsonResponseMapper.extractRating(aiResponse));
            String commentary = jsonResponseMapper.extractCommentary(aiResponse);
            if (commentary.isBlank()) {
                commentary = SPEECHLESS_COMMENTARY;
            }

            leaderboardService.updateRoundScoreForAPlayer(leaderboardId, playerId, score);
            results.add(new PlayerRoundResult(
                    player.getUserName(), submissionDTO.getText(), commentary, score, score > SURVIVAL_THRESHOLD));
            answeredPlayerIds.add(playerId);
        }

        for (Player player : round.getRoom().getPlayers()) {
            if (!answeredPlayerIds.contains(player.getId())) {
                leaderboardService.updateRoundScoreForAPlayer(leaderboardId, player.getId(), 0);
                results.add(new PlayerRoundResult(player.getUserName(), "", NO_ANSWER_COMMENTARY, 0, false));
            }
        }
        return results;
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
