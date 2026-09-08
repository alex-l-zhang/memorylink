-- V6：主动建立联系（搜索/发起/同意）与用户籍贯

ALTER TABLE users
    ADD COLUMN birth_place VARCHAR(100);

CREATE TABLE connection_requests (
    id                BIGSERIAL PRIMARY KEY,
    requester_id      BIGINT       NOT NULL REFERENCES users (id),
    target_id         BIGINT       NOT NULL REFERENCES users (id),
    requester_name    VARCHAR(50)  NOT NULL,
    birth_year        INT,
    birth_month       INT,
    birth_place       VARCHAR(100),
    relation          VARCHAR(30)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    responded_at      TIMESTAMPTZ,
    UNIQUE (requester_id, target_id)
);

CREATE INDEX idx_connection_requests_target ON connection_requests (target_id, status);

CREATE TABLE family_relationships (
    id              BIGSERIAL PRIMARY KEY,
    user_a_id       BIGINT       NOT NULL REFERENCES users (id),
    user_b_id       BIGINT       NOT NULL REFERENCES users (id),
    relation_a_to_b VARCHAR(30)  NOT NULL,
    relation_b_to_a VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id)
);
