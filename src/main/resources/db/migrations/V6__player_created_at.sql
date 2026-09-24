ALTER TABLE player
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

CREATE INDEX idx_player_created_at ON player (created_at);
