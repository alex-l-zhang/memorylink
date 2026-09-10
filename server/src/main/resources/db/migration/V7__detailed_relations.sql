-- V7：细化亲属关系（儿子/女儿/儿媳/女婿/爷爷/外婆…）+ 双向关系方向

ALTER TABLE connection_requests
    ADD COLUMN inverse_relation VARCHAR(30);

-- 旧数据：request.relation 语义为"发起人是目标的 X"，转换为 inverse 列保留；新语义见代码
UPDATE connection_requests
SET inverse_relation = CASE relation
        WHEN 'PARENT' THEN 'CHILD'
        WHEN 'CHILD' THEN 'PARENT'
        WHEN 'GRANDPARENT' THEN 'GRANDCHILD'
        WHEN 'GRANDCHILD' THEN 'GRANDPARENT'
        ELSE relation
    END
WHERE inverse_relation IS NULL;

-- 旧关系记录方向修正：CHILD/PARENT/GRANDCHILD/GRANDPARENT 双向互换
UPDATE family_relationships
SET relation_a_to_b = relation_b_to_a,
    relation_b_to_a = relation_a_to_b
WHERE relation_a_to_b IN ('CHILD', 'PARENT', 'GRANDCHILD', 'GRANDPARENT');

-- 邀请码加入的历史成员关系回填为双向关系（A=家族创建者/邀请人，B=成员）
INSERT INTO family_relationships
    (user_a_id, user_b_id, relation_a_to_b, relation_b_to_a, status, created_at)
SELECT f.creator_id,
       fm.user_id,
       COALESCE(fm.relation, 'OTHER'),
       CASE fm.relation
           WHEN 'PARENT' THEN 'CHILD'
           WHEN 'CHILD' THEN 'PARENT'
           WHEN 'GRANDPARENT' THEN 'GRANDCHILD'
           WHEN 'GRANDCHILD' THEN 'GRANDPARENT'
           ELSE COALESCE(fm.relation, 'OTHER')
       END,
       'ACTIVE',
       now()
FROM memorylink.family_members fm
JOIN memorylink.families f ON f.id = fm.family_id
WHERE fm.relation_source = 'INVITE_KEY'
  AND fm.user_id <> f.creator_id
  AND NOT EXISTS (
      SELECT 1 FROM memorylink.family_relationships r
      WHERE (r.user_a_id = f.creator_id AND r.user_b_id = fm.user_id)
         OR (r.user_a_id = fm.user_id AND r.user_b_id = f.creator_id)
  );
