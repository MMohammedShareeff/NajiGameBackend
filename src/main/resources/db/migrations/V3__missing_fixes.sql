ALTER TABLE player
    ADD COLUMN round_id BIGINT;


ALTER TABLE player
    ADD CONSTRAINT fk_player_round
        FOREIGN KEY (round_id) REFERENCES round (id) ON DELETE SET NULL;

CREATE INDEX idx_player_round_id ON player (round_id);

CREATE TABLE round_player
(
    round_id   BIGINT NOT NULL,
    players_id BIGINT NOT NULL,
    PRIMARY KEY (round_id, players_id),
    CONSTRAINT fk_round_player_round
        FOREIGN KEY (round_id) REFERENCES round (id) ON DELETE CASCADE,
    CONSTRAINT fk_round_player_player
        FOREIGN KEY (players_id) REFERENCES player (id) ON DELETE CASCADE
);

CREATE INDEX idx_round_player_players_id ON round_player (players_id);

CREATE TABLE round_submission
(
    round_id       BIGINT NOT NULL,
    submissions_id BIGINT NOT NULL,
    PRIMARY KEY (round_id, submissions_id),
    CONSTRAINT uk_round_submission_submissions
        UNIQUE (submissions_id),
    CONSTRAINT fk_round_submission_round
        FOREIGN KEY (round_id) REFERENCES round (id) ON DELETE CASCADE,
    CONSTRAINT fk_round_submission_submission
        FOREIGN KEY (submissions_id) REFERENCES submission (id) ON DELETE CASCADE
);

ALTER TABLE player
    ADD CONSTRAINT chk_player_last_game_status
        CHECK (last_game_status BETWEEN 0 AND 2);
