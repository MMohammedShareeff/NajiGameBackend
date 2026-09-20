-- ======================
-- SEQUENCES
-- ======================
CREATE SEQUENCE IF NOT EXISTS player_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS room_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS round_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS submission_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS dashboard_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS player_scores_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS leaderboard_seq START WITH 1 INCREMENT BY 1;

-- ======================
-- PLAYER
-- ======================
CREATE TABLE player (
    id                      BIGINT PRIMARY KEY DEFAULT nextval('player_seq'),
    username                VARCHAR(255) UNIQUE NOT NULL,
    password                VARCHAR(255) NOT NULL,
    email                   VARCHAR(255) UNIQUE NOT NULL,
    role                    VARCHAR(50),
    current_game_pass_code  VARCHAR(100),
    last_game_status        VARCHAR(20),
    leaderboard_id          BIGINT
);

CREATE INDEX user_name_idx ON player(username);
CREATE INDEX email_idx ON player(email);

-- ======================
-- LEADERBOARD
-- ======================
CREATE TABLE leaderboard (
    id BIGINT PRIMARY KEY DEFAULT nextval('leaderboard_seq')
);

-- ======================
-- ROOM
-- ======================
CREATE TABLE room (
    id              BIGINT PRIMARY KEY DEFAULT nextval('room_seq'),
    pass_code       VARCHAR(100) UNIQUE NOT NULL,
    is_active       BOOLEAN DEFAULT true,
    current_round   INTEGER DEFAULT 0,
    admin_id        BIGINT,
    leaderboard_id  BIGINT UNIQUE
);

-- Join table for Room <-> Player (Many-to-Many)
CREATE TABLE room_players (
    room_id     BIGINT NOT NULL,
    player_id   BIGINT NOT NULL,
    PRIMARY KEY (room_id, player_id)
);

-- ======================
-- ROUND
-- ======================
CREATE TABLE round (
    id          BIGINT PRIMARY KEY DEFAULT nextval('round_seq'),
    scenario    TEXT,
    no_of_round INTEGER,
    active      BOOLEAN DEFAULT false,
    room_id     BIGINT,
    leaderboard_id BIGINT
);

-- ======================
-- SUBMISSION
-- ======================
CREATE TABLE submission (
    id          BIGINT PRIMARY KEY DEFAULT nextval('submission_seq'),
    text        TEXT,
    processed   BOOLEAN DEFAULT false,
    round_id    BIGINT NOT NULL,
    player_id   BIGINT NOT NULL
);

-- ======================
-- DASHBOARD
-- ======================
CREATE TABLE dashboard (
    player_id               BIGINT PRIMARY KEY,
    total_games_played      INTEGER DEFAULT 0,
    total_games_won         INTEGER DEFAULT 0,
    total_games_lost        INTEGER DEFAULT 0,
    total_games_drawn       INTEGER DEFAULT 0,
    highest_score           DOUBLE PRECISION DEFAULT 0,
    average_score_per_round DOUBLE PRECISION DEFAULT 0,
    win_streak              INTEGER DEFAULT 0
);

-- ======================
-- PLAYER SCORES
-- ======================
CREATE TABLE player_scores (
    id              BIGINT PRIMARY KEY DEFAULT nextval('player_scores_seq'),
    player_id       BIGINT UNIQUE,
    leaderboard_id  BIGINT
);

-- ElementCollection for scores
CREATE TABLE player_scores_scores (
    player_scores_id BIGINT NOT NULL,
    scores           INTEGER
);

-- ======================
-- FOREIGN KEYS
-- ======================
ALTER TABLE player
    ADD CONSTRAINT fk_player_leaderboard
    FOREIGN KEY (leaderboard_id) REFERENCES leaderboard(id);

ALTER TABLE room
    ADD CONSTRAINT fk_room_admin
    FOREIGN KEY (admin_id) REFERENCES player(id);

ALTER TABLE room
    ADD CONSTRAINT fk_room_leaderboard
    FOREIGN KEY (leaderboard_id) REFERENCES leaderboard(id);

ALTER TABLE room_players
    ADD CONSTRAINT fk_room_players_room
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE;

ALTER TABLE room_players
    ADD CONSTRAINT fk_room_players_player
    FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE;

ALTER TABLE round
    ADD CONSTRAINT fk_round_room
    FOREIGN KEY (room_id) REFERENCES room(id);

ALTER TABLE round
    ADD CONSTRAINT fk_round_leaderboard
    FOREIGN KEY (leaderboard_id) REFERENCES leaderboard(id);

ALTER TABLE submission
    ADD CONSTRAINT fk_submission_round
    FOREIGN KEY (round_id) REFERENCES round(id);

ALTER TABLE submission
    ADD CONSTRAINT fk_submission_player
    FOREIGN KEY (player_id) REFERENCES player(id);

ALTER TABLE dashboard
    ADD CONSTRAINT fk_dashboard_player
    FOREIGN KEY (player_id) REFERENCES player(id);

ALTER TABLE player_scores
    ADD CONSTRAINT fk_player_scores_player
    FOREIGN KEY (player_id) REFERENCES player(id);

ALTER TABLE player_scores
    ADD CONSTRAINT fk_player_scores_leaderboard
    FOREIGN KEY (leaderboard_id) REFERENCES leaderboard(id);

ALTER TABLE player_scores_scores
    ADD CONSTRAINT fk_player_scores_scores
    FOREIGN KEY (player_scores_id) REFERENCES player_scores(id) ON DELETE CASCADE;