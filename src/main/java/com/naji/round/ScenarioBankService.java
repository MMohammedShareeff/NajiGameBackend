package com.naji.round;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScenarioBankService {

    public static final int SLOT_COUNT = 5;

    public record BankScenario(String theme, String text) {
    }

    private final JdbcTemplate jdbcTemplate;

    public Optional<BankScenario> pick(int roundNumber, String lang, List<String> avoidTexts) {
        int slot = slotFor(roundNumber);
        StringBuilder sql = new StringBuilder("SELECT id, theme, text FROM scenario_bank WHERE lang = ? AND slot = ?");
        List<Object> params = new ArrayList<>();
        params.add(lang);
        params.add(slot);
        if (!avoidTexts.isEmpty()) {
            sql.append(" AND text NOT IN (").append(String.join(",", avoidTexts.stream().map(text -> "?").toList())).append(")");
            params.addAll(avoidTexts);
        }
        sql.append(" ORDER BY uses, random() LIMIT 1");

        List<Object[]> rows = jdbcTemplate.query(sql.toString(),
                (row, index) -> new Object[]{row.getLong("id"), row.getString("theme"), row.getString("text")},
                params.toArray());
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] chosen = rows.get(0);
        jdbcTemplate.update("UPDATE scenario_bank SET uses = uses + 1 WHERE id = ?", chosen[0]);
        return Optional.of(new BankScenario((String) chosen[1], (String) chosen[2]));
    }

    public Map<String, String> translations(String text) {
        Map<String, String> byLanguage = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT b.lang, b.text FROM scenario_bank a JOIN scenario_bank b ON b.id = a.id OR b.id = a.pair_id "
                        + "WHERE a.text = ?",
                row -> {
                    byLanguage.put(row.getString("lang"), row.getString("text"));
                },
                text);
        return byLanguage;
    }

    public int leastUsedCount(int roundNumber, String lang) {
        Integer minimum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MIN(uses), 0) FROM scenario_bank WHERE lang = ? AND slot = ?", Integer.class, lang, slotFor(roundNumber));
        return minimum == null ? 0 : minimum;
    }

    public List<String> recentTexts(int roundNumber, String lang, int limit) {
        return jdbcTemplate.queryForList(
                "SELECT text FROM scenario_bank WHERE lang = ? AND slot = ? ORDER BY created_at DESC, id DESC LIMIT ?",
                String.class, lang, slotFor(roundNumber), limit);
    }

    public void add(int roundNumber, String lang, String theme, String text) {
        jdbcTemplate.update(
                "INSERT INTO scenario_bank (lang, slot, theme, text) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING",
                lang, slotFor(roundNumber), theme, text);
    }

    private static int slotFor(int roundNumber) {
        return Math.min(Math.max(roundNumber, 1), SLOT_COUNT);
    }
}
