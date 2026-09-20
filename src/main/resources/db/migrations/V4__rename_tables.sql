ALTER TABLE round_player RENAME TO round_players;
ALTER TABLE round_players RENAME CONSTRAINT round_player_pkey        TO round_players_pkey;
ALTER TABLE round_players RENAME CONSTRAINT fk_round_player_round    TO fk_round_players_round;
ALTER TABLE round_players RENAME CONSTRAINT fk_round_player_player   TO fk_round_players_player;
ALTER INDEX idx_round_player_players_id RENAME TO idx_round_players_players_id;

ALTER TABLE round_submission RENAME TO round_submissions;
ALTER TABLE round_submissions RENAME CONSTRAINT round_submission_pkey               TO round_submissions_pkey;
ALTER TABLE round_submissions RENAME CONSTRAINT uk_round_submission_submissions     TO uk_round_submissions_submissions;
ALTER TABLE round_submissions RENAME CONSTRAINT fk_round_submission_round           TO fk_round_submissions_round;
ALTER TABLE round_submissions RENAME CONSTRAINT fk_round_submission_submission      TO fk_round_submissions_submission;
