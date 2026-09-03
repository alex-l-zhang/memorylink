-- V4：在世人物档案与本人账号绑定（AI 讲述本人开启的前提）

ALTER TABLE loved_ones
    ADD COLUMN user_id BIGINT REFERENCES users (id);

CREATE INDEX idx_loved_ones_user ON loved_ones (user_id);
