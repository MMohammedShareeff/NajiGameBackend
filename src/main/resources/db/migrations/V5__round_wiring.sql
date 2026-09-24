DO $$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_attribute a
          ON a.attrelid = con.conrelid AND a.attnum = ANY (con.conkey)
        WHERE con.conrelid = 'player_scores'::regclass
          AND con.contype = 'u'
        GROUP BY con.oid, con.conname
        HAVING count(*) = 1 AND bool_and(a.attname = 'player_id')
    LOOP
        EXECUTE format('ALTER TABLE player_scores DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE player_scores
    ADD CONSTRAINT uk_player_scores_player_leaderboard UNIQUE (player_id, leaderboard_id);