package com.naji.round;


import com.naji.leaderboard.LeaderboardService;
import com.naji.openai.Contestant;
import com.naji.openai.GameLanguage;
import com.naji.openai.JsonResponseMapper;
import com.naji.openai.JudgeVerdict;
import com.naji.openai.OpenAiService;
import com.naji.openai.ScenarioTheme;
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
import java.util.Objects;
import java.util.Optional;
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
    private static final String SPEECHLESS_COMMENTARY_AR = "حتى الذكاء الاصطناعي بقي بلا كلام أمام هذه الخطة.";
    private static final String NO_ANSWER_COMMENTARY_AR =
            "لا خطة ولا جواب ولا نجاة. الخطر أكل غداءه مجانا بينما كان هذا المتسابق ما زال يفكر.";

    private final RoundRepository roundRepository;
    private final OpenAiService openAiService;
    private final PlayerRepository playerRepository;
    private final JsonResponseMapper jsonResponseMapper;
    private final LeaderboardService leaderboardService;
    private final ScenarioBankService scenarioBank;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @Getter
    private final Map<String, String> responses = new HashMap<>();

    public Boolean getStatus(Long roundId) {
        return roundRepository.findById(roundId)
                .map(Round::getActive)
                .orElse(false);
    }

    @Transactional
    public String startRound(Round round, ScenarioTheme fallbackTheme, int totalRounds, String lang) {
        round.setActive(true);
        List<String> earlierScenarios = roundRepository.findTop4ByRoomIdOrderByIdDesc(round.getRoom().getId()).stream()
                .map(Round::getScenario)
                .filter(Objects::nonNull)
                .toList();

        Optional<ScenarioBankService.BankScenario> banked = scenarioBank.pick(round.getNoOfRound(), lang, earlierScenarios);
        String themeLabel = GameLanguage.themeLabel(lang, round.getNoOfRound(), fallbackTheme);
        if (banked.isPresent()) {
            round.setScenario(banked.get().text());
            themeLabel = banked.get().theme();
        } else {
            round.setScenario(openAiService.getScenario(fallbackTheme, round.getNoOfRound(), totalRounds, earlierScenarios, lang));
        }
        roundRepository.save(round);
        return themeLabel;
    }

    public Map<String, String> scenarioTranslations(String scenario) {
        return scenarioBank.translations(scenario);
    }

    @Transactional
    public void endRound(Round round) {

        round.setActive(false);
        roundRepository.save(round);
//        shutdownScheduler();
    }

    public List<PlayerRoundResult> processSubmissions(Long roundId, String lang) {
        String noAnswerCommentary = GameLanguage.isArabic(lang) ? NO_ANSWER_COMMENTARY_AR : NO_ANSWER_COMMENTARY;
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("round with id equals %d roundId not found", roundId)));

        Long leaderboardId = round.getRoom().getLeaderboard().getId();
        List<PlayerRoundResult> results = new ArrayList<>();
        Set<Long> answeredPlayerIds = new HashSet<>();

        List<SubmissionDTO> submissions = getAllSubmissionsInRound(roundId);
        List<Player> submitters = new ArrayList<>();
        List<Contestant> contestants = new ArrayList<>();
        for (SubmissionDTO submissionDTO : submissions) {
            Long playerId = submissionDTO.getPlayerId();
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("player with id equals %d not found", playerId)));
            submitters.add(player);
            contestants.add(new Contestant(player.getUserName(), submissionDTO.getText()));
        }

        List<Contestant> toJudge = contestants.stream()
                .filter(contestant -> contestant.plan() != null && !contestant.plan().isBlank())
                .toList();
        List<JudgeVerdict> batchVerdicts = openAiService.judgeRound(round.getScenario(), toJudge, lang);
        int judgedIndex = 0;

        for (int index = 0; index < submissions.size(); index++) {
            SubmissionDTO submissionDTO = submissions.get(index);
            Player player = submitters.get(index);
            Contestant contestant = contestants.get(index);

            JudgeVerdict verdict;
            if (contestant.plan() == null || contestant.plan().isBlank()) {
                verdict = new JudgeVerdict(0, noAnswerCommentary);
            } else {
                verdict = batchVerdicts.get(judgedIndex++);
                if (verdict == null) {
                    verdict = judgeSingly(round.getScenario(), contestant, lang);
                }
            }

            leaderboardService.updateRoundScoreForAPlayer(leaderboardId, player.getId(), verdict.rating());
            results.add(new PlayerRoundResult(player.getUserName(), submissionDTO.getText(), verdict.commentary(),
                    verdict.rating(), verdict.rating() > SURVIVAL_THRESHOLD));
            answeredPlayerIds.add(player.getId());
        }

        for (Player player : round.getRoom().getPlayers()) {
            if (!answeredPlayerIds.contains(player.getId())) {
                leaderboardService.updateRoundScoreForAPlayer(leaderboardId, player.getId(), 0);
                results.add(new PlayerRoundResult(player.getUserName(), "", noAnswerCommentary, 0, false));
            }
        }
        return results;
    }

    private JudgeVerdict judgeSingly(String scenario, Contestant contestant, String lang) {
        String aiResponse = openAiService.getResponse(scenario, contestant.plan(), contestant.name(), lang);
        int score = Math.max(0, jsonResponseMapper.extractRating(aiResponse));
        String commentary = jsonResponseMapper.extractCommentary(aiResponse);
        String speechless = GameLanguage.isArabic(lang) ? SPEECHLESS_COMMENTARY_AR : SPEECHLESS_COMMENTARY;
        return new JudgeVerdict(score, commentary.isBlank() ? speechless : commentary);
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
