-- V5：口述历史可见性（默认仅本人可见；故人口述默认家族可见）

ALTER TABLE oral_histories
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'FAMILY';
