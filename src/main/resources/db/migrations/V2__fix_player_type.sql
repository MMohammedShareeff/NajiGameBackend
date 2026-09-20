ALTER TABLE player
ALTER COLUMN last_game_status TYPE SMALLINT
USING last_game_status::SMALLINT;