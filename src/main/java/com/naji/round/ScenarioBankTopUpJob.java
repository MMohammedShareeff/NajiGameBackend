package com.naji.round;

import com.naji.openai.OpenAiService;
import com.naji.openai.ScenarioTheme;
import com.naji.openai.ScenarioThemes;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScenarioBankTopUpJob {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioBankTopUpJob.class);
    private static final int MIN_USES_BEFORE_TOP_UP = 2;
    private static final int RECENT_TEXTS_SHOWN_TO_AI = 8;

    private final ScenarioBankService bank;
    private final OpenAiService openAiService;

    @Scheduled(cron = "${scenario.bank-top-up-cron:0 30 3 * * *}")
    public void addFreshScenarios() {
        for (int slot = 1; slot <= ScenarioBankService.SLOT_COUNT; slot++) {
            try {
                if (bank.leastUsedCount(slot) < MIN_USES_BEFORE_TOP_UP) {
                    continue;
                }
                ScenarioTheme theme = ScenarioThemes.forRound(slot);
                List<String> recent = bank.recentTexts(slot, RECENT_TEXTS_SHOWN_TO_AI);
                String scenario = openAiService.getScenario(theme, slot, ScenarioBankService.SLOT_COUNT, recent);
                bank.add(slot, theme.label(), scenario);
                logger.info("Added a new scenario to slot {}", slot);
            } catch (RuntimeException ex) {
                logger.warn("Could not add a scenario to slot {}: {}", slot, ex.getMessage());
            }
        }
    }
}
