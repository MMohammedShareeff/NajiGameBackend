package com.naji.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GuestCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(GuestCleanupJob.class);

    private static final String EXPIRED_GUEST_IDS = """
            SELECT p.id FROM player p
            WHERE p.email LIKE '%@guest.naji.local'
              AND p.created_at < now() - make_interval(hours => ?)
              AND p.id NOT IN (SELECT player_id FROM room_players)
              AND p.id NOT IN (SELECT admin_id FROM room WHERE admin_id IS NOT NULL)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final int retentionHours;

    public GuestCleanupJob(JdbcTemplate jdbcTemplate, @Value("${guest.retention-hours:48}") int retentionHours) {
        this.jdbcTemplate = jdbcTemplate;
        this.retentionHours = retentionHours;
    }

    @Scheduled(cron = "${guest.cleanup-cron:0 15 * * * *}")
    @Transactional
    public int purgeExpiredGuests() {
        List<Long> guestIds = jdbcTemplate.queryForList(EXPIRED_GUEST_IDS, Long.class, retentionHours);
        if (guestIds.isEmpty()) {
            return 0;
        }

        String idList = guestIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbcTemplate.update("DELETE FROM submission WHERE player_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM player_scores WHERE player_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM dashboard WHERE player_id IN (" + idList + ")");
        jdbcTemplate.update("DELETE FROM player WHERE id IN (" + idList + ")");

        logger.info("Purged {} expired guest accounts", guestIds.size());
        return guestIds.size();
    }
}
