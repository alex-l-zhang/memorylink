-- V2：邀请码与成员证据状态

CREATE TABLE invite_keys (
    id          BIGSERIAL PRIMARY KEY,
    loved_one_id BIGINT       NOT NULL REFERENCES loved_ones (id),
    code_hash   VARCHAR(64)  NOT NULL UNIQUE,
    created_by  BIGINT       NOT NULL REFERENCES users (id),
    role        VARCHAR(20)  NOT NULL DEFAULT 'VIEWER',
    expires_at  TIMESTAMPTZ  NOT NULL,
    max_uses    INT          NOT NULL DEFAULT 1,
    used_count  INT          NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    used_at     TIMESTAMPTZ
);

CREATE INDEX idx_invite_keys_loved_one ON invite_keys (loved_one_id);

ALTER TABLE family_members
    ADD COLUMN evidence_status VARCHAR(20) NOT NULL DEFAULT 'SELF_DECLARED',
    ADD COLUMN relation_source VARCHAR(30);
