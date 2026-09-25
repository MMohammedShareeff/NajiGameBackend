ALTER TABLE scenario_bank ADD COLUMN pair_id BIGINT;

WITH numbered AS (
    SELECT id, lang, slot, row_number() OVER (PARTITION BY lang, slot ORDER BY id) AS n
    FROM scenario_bank
)
UPDATE scenario_bank target
SET pair_id = other.id
FROM numbered me
JOIN numbered other ON other.slot = me.slot AND other.n = me.n AND other.lang <> me.lang
WHERE target.id = me.id;
