CREATE TABLE friend (
    player_id  BIGINT NOT NULL REFERENCES player (id) ON DELETE CASCADE,
    friend_id  BIGINT NOT NULL REFERENCES player (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, friend_id),
    CONSTRAINT chk_friend_not_self CHECK (player_id <> friend_id)
);

CREATE INDEX idx_friend_friend_id ON friend (friend_id);
