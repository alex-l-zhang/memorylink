-- V9：每位用户只保留一份"本人档案"，多家族成员卡合并

CREATE TEMPORARY TABLE tmp_self_keep ON COMMIT DROP AS
SELECT base.user_id,
       COALESCE(owner_card.keep_id, any_card.keep_id) AS keep_id
FROM (
    SELECT DISTINCT user_id FROM loved_ones
    WHERE user_id IS NOT NULL AND is_deceased = FALSE
) base
LEFT JOIN (
    SELECT lo.user_id, MIN(lo.id) AS keep_id
    FROM loved_ones lo
    JOIN families f ON f.id = lo.family_id
    WHERE lo.user_id IS NOT NULL AND lo.is_deceased = FALSE AND f.creator_id = lo.user_id
    GROUP BY lo.user_id
) owner_card ON owner_card.user_id = base.user_id
JOIN (
    SELECT user_id, MIN(id) AS keep_id
    FROM loved_ones
    WHERE user_id IS NOT NULL AND is_deceased = FALSE
    GROUP BY user_id
) any_card ON any_card.user_id = base.user_id;

CREATE TEMPORARY TABLE tmp_self_dup ON COMMIT DROP AS
SELECT lo.id AS dup_id, k.keep_id
FROM loved_ones lo
JOIN tmp_self_keep k ON k.user_id = lo.user_id
WHERE lo.user_id IS NOT NULL AND lo.is_deceased = FALSE AND lo.id <> k.keep_id;

UPDATE media_files m SET loved_one_id = d.keep_id FROM tmp_self_dup d WHERE m.loved_one_id = d.dup_id;
UPDATE events e SET loved_one_id = d.keep_id FROM tmp_self_dup d WHERE e.loved_one_id = d.dup_id;
UPDATE conversations c SET loved_one_id = d.keep_id FROM tmp_self_dup d WHERE c.loved_one_id = d.dup_id;
UPDATE consent_records cr SET loved_one_id = d.keep_id FROM tmp_self_dup d WHERE cr.loved_one_id = d.dup_id;
UPDATE oral_histories o SET loved_one_id = d.keep_id FROM tmp_self_dup d WHERE o.loved_one_id = d.dup_id;

DELETE FROM loved_ones lo USING tmp_self_dup d WHERE lo.id = d.dup_id;
