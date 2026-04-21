-- schema.sql for PostgreSQL

-- 1. 기초 테이블 (참조를 하지 않는 테이블들)
DROP TABLE IF EXISTS users;
CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    user_type     INTEGER   NOT NULL,
    nickname      VARCHAR   NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    deleted_at    TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_user_nickname_active ON users (nickname) WHERE deleted_at IS NULL;

DROP TABLE IF EXISTS users_authentication;
CREATE TABLE users_authentication
(
    id             UUID PRIMARY KEY,
    user_id        UUID      NOT NULL,
    login_provider INTEGER   NOT NULL,
    identifier     VARCHAR   NOT NULL,
    credential     VARCHAR,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    deleted_at     TIMESTAMP,

    CONSTRAINT fk_users_authentication_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_authentication_identifier_active ON users_authentication (identifier) WHERE deleted_at IS NULL;

DROP TABLE IF EXISTS post_providers;
CREATE TABLE post_providers
(
    id          UUID PRIMARY KEY,
    name        VARCHAR   NOT NULL,
    description VARCHAR   NOT NULL,
    base_url    VARCHAR   NOT NULL,
    is_used     BOOLEAN   NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    deleted_at  TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_post_providers_name_active ON post_providers (name) WHERE deleted_at IS NULL;

DROP TABLE IF EXISTS collect_sources;
CREATE TABLE collect_sources
(
    id               UUID PRIMARY KEY,
    post_provider_id UUID      NOT NULL,
    cron_expression  VARCHAR(50),
    is_used          BOOLEAN   NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,
    deleted_at       TIMESTAMP,

    CONSTRAINT fk_collect_sources_post_provider_id FOREIGN KEY (post_provider_id) REFERENCES post_providers (id)
);

DROP TABLE IF EXISTS report_types;
CREATE TABLE report_types
(
    code       INTEGER PRIMARY KEY,
    category   VARCHAR,
    name       VARCHAR,
    is_used    BOOLEAN   NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS posts;
CREATE TABLE posts
(
    id                 UUID PRIMARY KEY,
    provider_id        UUID                NOT NULL REFERENCES post_providers (id),
    title              VARCHAR             NOT NULL,
    url                VARCHAR(512) UNIQUE NOT NULL,
    published_at       TIMESTAMP           NOT NULL,
    thumbnail_url      VARCHAR             NOT NULL,
    summary            TEXT                NOT NULL,
    like_count         INTEGER DEFAULT 0,
    view_count         INTEGER DEFAULT 0,
    comment_count      INTEGER DEFAULT 0,
    total_report_count INTEGER DEFAULT 0,
    status             INTEGER DEFAULT 0,
    created_at         TIMESTAMP           NOT NULL,
    updated_at         TIMESTAMP           NOT NULL
);

CREATE INDEX idx_posts_title_provider ON posts (title, provider_id);
CREATE INDEX idx_posts_provider_id ON posts (provider_id);

-- 3. 수집 및 작업 관련 테이블
DROP TABLE IF EXISTS collecting_jobs;
CREATE TABLE collecting_jobs
(
    id                UUID PRIMARY KEY,
    collect_source_id UUID REFERENCES collect_sources (id),
    status            VARCHAR(20),
    total_count       INTEGER DEFAULT 0,
    collected_count   INTEGER DEFAULT 0,
    error_message     TEXT,
    started_at        TIMESTAMP,
    ended_at          TIMESTAMP
);

DROP TABLE IF EXISTS collect_source_posts;
CREATE TABLE collect_source_posts
(
    id                     UUID PRIMARY KEY,
    source_id              UUID REFERENCES collect_sources (id),
    title                  VARCHAR             NOT NULL,
    url                    VARCHAR(512) UNIQUE NOT NULL,
    published_at           TIMESTAMP           NOT NULL,
    thumbnail_url          VARCHAR             NOT NULL,
    summary                TEXT                NOT NULL,
    content                TEXT                NOT NULL,
    content_hash           VARCHAR             NOT NULL,
    indexing_status        INTEGER   DEFAULT 0,
    indexing_error_count   INTEGER   DEFAULT 0,
    last_indexed_at        TIMESTAMP DEFAULT NULL,
    last_collected_at      TIMESTAMP DEFAULT NOW(),
    last_collecting_job_id UUID
);

DROP TABLE IF EXISTS indexing_jobs;
CREATE TABLE indexing_jobs
(
    id                UUID PRIMARY KEY,
    collecting_job_id UUID REFERENCES collecting_jobs (id),
    job_type          VARCHAR(20),
    status            VARCHAR(20),
    total_count       INTEGER DEFAULT 0,
    indexed_count     INTEGER DEFAULT 0,
    error_message     TEXT,
    started_at        TIMESTAMP,
    ended_at          TIMESTAMP
);

-- 4. 사용자 활동 관련 테이블 (post, user 참조)
DROP TABLE IF EXISTS post_likes;
CREATE TABLE post_likes
(
    id         UUID PRIMARY KEY,
    user_id    UUID      NOT NULL REFERENCES users (id),
    post_id    UUID      NOT NULL REFERENCES posts (id),
    is_enable  BOOLEAN   NOT NULL,
    created_at TIMESTAMP NOT NULL,

    -- UNIQUE 제약 조건 추가
    CONSTRAINT uk_post_likes_user_id_post_id UNIQUE (user_id, post_id)
);

DROP TABLE IF EXISTS post_bookmarks;
CREATE TABLE post_bookmarks
(
    id         UUID PRIMARY KEY,
    user_id    UUID      NOT NULL REFERENCES users (id),
    post_id    UUID      NOT NULL REFERENCES posts (id),
    is_enable  BOOLEAN   NOT NULL,
    created_at TIMESTAMP NOT NULL,

    -- UNIQUE 제약 조건 추가
    CONSTRAINT uk_post_bookmarks_user_id_post_id UNIQUE (user_id, post_id)
);

DROP TABLE IF EXISTS post_comments;
CREATE TABLE post_comments
(
    id                 UUID PRIMARY KEY,
    user_id            UUID      NOT NULL REFERENCES users (id),
    post_id            UUID      NOT NULL REFERENCES posts (id),
    parent_comment_id  UUID REFERENCES post_comments (id),
    content            VARCHAR   NOT NULL,
    total_report_count INTEGER DEFAULT 0,
    status             INTEGER DEFAULT 0,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL
);

-- 5. 신고 관련 테이블
DROP TABLE IF EXISTS post_reports;
CREATE TABLE post_reports
(
    id               UUID PRIMARY KEY,
    user_id          UUID      NOT NULL REFERENCES users (id),
    post_id          UUID      NOT NULL REFERENCES posts (id),
    report_type_code INTEGER   NOT NULL REFERENCES report_types (code),
    content          VARCHAR   NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    processed        BOOLEAN   NOT NULL
);

DROP TABLE IF EXISTS comment_reports;
CREATE TABLE comment_reports
(
    id               UUID PRIMARY KEY,
    comment_id       UUID      NOT NULL REFERENCES post_comments (id),
    user_id          UUID      NOT NULL REFERENCES users (id),
    report_type_code INTEGER   NOT NULL REFERENCES report_types (code),
    content          VARCHAR   NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    processed        BOOLEAN   NOT NULL
);
