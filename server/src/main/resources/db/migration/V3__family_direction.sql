-- V3：家族关系方向——人物双态、AI 讲述开关、口述历史

ALTER TABLE loved_ones
    ADD COLUMN is_deceased        BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN ai_persona_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN ai_enabled_by      BIGINT       REFERENCES users (id),
    ADD COLUMN ai_enabled_at      TIMESTAMPTZ;

CREATE TABLE oral_histories (
    id            BIGSERIAL PRIMARY KEY,
    loved_one_id  BIGINT       NOT NULL REFERENCES loved_ones (id),
    media_file_id BIGINT       NOT NULL UNIQUE REFERENCES media_files (id),
    title         VARCHAR(100),
    transcript    TEXT,
    uploaded_by   BIGINT       NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_oral_histories_person ON oral_histories (loved_one_id);
