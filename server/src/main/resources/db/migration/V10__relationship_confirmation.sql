-- V10：关系自动建立、展示需确认 + 站内消息中心

ALTER TABLE family_relationships
    ADD COLUMN a_status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN b_status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN a_confirmed_at TIMESTAMPTZ,
    ADD COLUMN b_confirmed_at TIMESTAMPTZ,
    ALTER COLUMN relation_a_to_b DROP NOT NULL,
    ALTER COLUMN relation_b_to_a DROP NOT NULL;

CREATE TABLE notifications (
    id                 BIGSERIAL PRIMARY KEY,
    recipient_id       BIGINT       NOT NULL REFERENCES users (id),
    type               VARCHAR(30)  NOT NULL,
    relationship_id    BIGINT       REFERENCES family_relationships (id),
    other_user_id      BIGINT       REFERENCES users (id),
    title              VARCHAR(100) NOT NULL,
    body               VARCHAR(500),
    suggested_relation VARCHAR(30),
    status             VARCHAR(20)  NOT NULL DEFAULT 'UNREAD',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    read_at            TIMESTAMPTZ,
    actioned_at        TIMESTAMPTZ
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, status);
