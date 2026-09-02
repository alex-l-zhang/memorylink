-- 忆联 MemoryLink V1 初始表结构
-- 库: memorylink / schema: memorylink

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    phone         VARCHAR(20)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    birth_date    DATE,
    age_group     VARCHAR(20),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE families (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    creator_id BIGINT       NOT NULL REFERENCES users (id),
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE family_members (
    id         BIGSERIAL PRIMARY KEY,
    family_id  BIGINT      NOT NULL REFERENCES families (id),
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    relation   VARCHAR(30),
    role       VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (family_id, user_id)
);

CREATE TABLE loved_ones (
    id          BIGSERIAL PRIMARY KEY,
    family_id   BIGINT      NOT NULL REFERENCES families (id),
    name        VARCHAR(50) NOT NULL,
    birth_date  DATE,
    death_date  DATE,
    birth_place VARCHAR(100),
    bio         TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by  BIGINT      REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE media_files (
    id          BIGSERIAL PRIMARY KEY,
    loved_one_id BIGINT      NOT NULL REFERENCES loved_ones (id),
    uploader_id BIGINT      REFERENCES users (id),
    media_type  VARCHAR(20) NOT NULL,
    object_key  VARCHAR(255) NOT NULL,
    checksum    VARCHAR(64),
    size_bytes  BIGINT,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_media_loved_one ON media_files (loved_one_id);

CREATE TABLE events (
    id          BIGSERIAL PRIMARY KEY,
    loved_one_id BIGINT      NOT NULL REFERENCES loved_ones (id),
    event_type  VARCHAR(30) NOT NULL,
    event_date  DATE        NOT NULL,
    repeat_rule VARCHAR(30) NOT NULL DEFAULT 'YEARLY',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_loved_one ON events (loved_one_id);

CREATE TABLE albums (
    id          BIGSERIAL PRIMARY KEY,
    family_id   BIGINT      NOT NULL REFERENCES families (id),
    template_id VARCHAR(50),
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    pdf_key     VARCHAR(255),
    created_by  BIGINT      REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversations (
    id           BIGSERIAL PRIMARY KEY,
    loved_one_id BIGINT      NOT NULL REFERENCES loved_ones (id),
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    question     TEXT        NOT NULL,
    answer       TEXT,
    ai_flag      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_loved_one ON conversations (loved_one_id, created_at);

CREATE TABLE consent_records (
    id            BIGSERIAL PRIMARY KEY,
    loved_one_id  BIGINT      NOT NULL REFERENCES loved_ones (id),
    consent_type  VARCHAR(30) NOT NULL,
    consentor_ids JSONB       NOT NULL DEFAULT '[]',
    signed_at     TIMESTAMPTZ,
    file_key      VARCHAR(255),
    status        VARCHAR(20) NOT NULL DEFAULT 'VALID',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_consent_loved_one ON consent_records (loved_one_id);

CREATE TABLE audit_logs (
    id         BIGSERIAL PRIMARY KEY,
    actor_type VARCHAR(20)  NOT NULL,
    actor_id   BIGINT,
    action     VARCHAR(50)  NOT NULL,
    target     VARCHAR(100),
    detail     JSONB,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_actor ON audit_logs (actor_type, actor_id);
CREATE INDEX idx_audit_created ON audit_logs (created_at);

CREATE TABLE partner_organizations (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    settle_ratio_json JSONB        NOT NULL DEFAULT '{}',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE partner_staff (
    id         BIGSERIAL PRIMARY KEY,
    org_id     BIGINT      NOT NULL REFERENCES partner_organizations (id),
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    role       VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, user_id)
);

CREATE TABLE partner_orders (
    id              BIGSERIAL PRIMARY KEY,
    org_id          BIGINT         NOT NULL REFERENCES partner_organizations (id),
    order_no        VARCHAR(64)    NOT NULL UNIQUE,
    order_type      VARCHAR(30)    NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    platform_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    org_amount      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(100)   NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_org ON partner_orders (org_id);

CREATE TABLE admin_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'OPERATOR',
    totp_secret   VARCHAR(100),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE delete_requests (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    loved_one_id BIGINT      NOT NULL REFERENCES loved_ones (id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    handler_id   BIGINT      REFERENCES admin_users (id),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_delete_requests_status ON delete_requests (status, requested_at);

CREATE TABLE work_orders (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(30) NOT NULL,
    priority        VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assignee_id     BIGINT      REFERENCES admin_users (id),
    reporter_user_id BIGINT     REFERENCES users (id),
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_work_orders_status ON work_orders (status, created_at);
