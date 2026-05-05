-- schema.sql for PostgreSQL

-- 0. 기존 테이블 모두 제거 (의존성 역순 + CASCADE)
-- collect_source_posts <-> indexing_jobs 양방향 FK 때문에 CASCADE 필수
DROP TABLE IF EXISTS comment_reports CASCADE;
DROP TABLE IF EXISTS post_reports CASCADE;
DROP TABLE IF EXISTS post_comments CASCADE;
DROP TABLE IF EXISTS post_bookmarks CASCADE;
DROP TABLE IF EXISTS post_likes CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS report_types CASCADE;
DROP TABLE IF EXISTS collect_source_posts CASCADE;
DROP TABLE IF EXISTS indexing_jobs CASCADE;
DROP TABLE IF EXISTS collecting_jobs CASCADE;
DROP TABLE IF EXISTS collect_sources CASCADE;
DROP TABLE IF EXISTS post_providers CASCADE;
DROP TABLE IF EXISTS users_authentication CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. 기초 테이블 (참조를 하지 않는 테이블들)
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

CREATE TABLE collect_sources
(
    id                    UUID PRIMARY KEY,
    post_provider_id      UUID        NOT NULL,
    url                   VARCHAR,
    collect_schedule_type VARCHAR(20) NOT NULL, -- CRON / MANUAL
    cron_expression       VARCHAR(50),
    is_used               BOOLEAN     NOT NULL,
    created_at            TIMESTAMP   NOT NULL,
    updated_at            TIMESTAMP   NOT NULL,
    deleted_at            TIMESTAMP,

    CONSTRAINT fk_collect_sources_post_provider_id FOREIGN KEY (post_provider_id) REFERENCES post_providers (id)
);

CREATE TABLE collecting_jobs
(
    id                UUID PRIMARY KEY,
    collect_source_id UUID        NOT NULL,
    from_page         INTEGER     NOT NULL,
    to_page           INTEGER     NOT NULL,
    job_status        VARCHAR(20) NOT NULL,
    collecting_status VARCHAR(20),
    triggered_by      UUID,
    total_count       INTEGER DEFAULT 0,
    collected_count   INTEGER DEFAULT 0,
    attempt_count     INTEGER DEFAULT 0,
    error_message     TEXT,
    started_at        TIMESTAMP,
    ended_at          TIMESTAMP,
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL,
    deleted_at        TIMESTAMP,

    CONSTRAINT fk_collecting_jobs_collect_source_id FOREIGN KEY (collect_source_id) REFERENCES collect_sources (id)
);

-- 중복 방지 unique index(PENDING 또는 RUNNING이 이미 있으면 또 만들지 않음)
-- 같은 collect_source에 대해 active Job이 두 개 동시 존재 불가.
-- INSERT 시도 시 unique violation으로 막힌다. unique violation 잡으면 그냥 무시하거나 "이미 진행중" 에러로 변환.
CREATE UNIQUE INDEX IF NOT EXISTS uk_collecting_jobs_collect_source_id_job_status ON collecting_jobs (collect_source_id) WHERE job_status IN ('PENDING', 'RUNNING');

CREATE TABLE indexing_jobs
(
    id                UUID PRIMARY KEY,
    indexing_job_type VARCHAR(20) NOT NULL, -- CRON / MANUAL
    job_status        VARCHAR(20) NOT NULL, -- PENDING/RUNNING/SUCCESS/FAILED
    total_count       INTEGER DEFAULT 0,
    indexed_count     INTEGER DEFAULT 0,
    error_message     TEXT,
    started_at        TIMESTAMP,
    ended_at          TIMESTAMP,
    triggered_by      UUID,                 -- MANUAL일 때 사용자 (선택)
    target_source_id  UUID,                 -- MANUAL 재색인 대상 source (선택)
    target_post_id    UUID,                 -- MANUAL 재색인 대상 post (선택)
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL,
    deleted_at        TIMESTAMP,

    CONSTRAINT fk_indexing_jobs_target_source_id FOREIGN KEY (target_source_id) REFERENCES collect_sources (id)
);

-- 중복 방지 unique index(PENDING 또는 RUNNING이 이미 있으면 또 만들지 않음)
-- 같은 collect_source에 대해 active Job이 두 개 동시 존재 불가.
-- 같은 collect_source_post에 대해 active Job이 두 개 동시 존재 불가.
-- INSERT 시도 시 unique violation으로 막힌다. unique violation 잡으면 그냥 무시하거나 "이미 진행중" 에러로 변환.
-- 2 == MANUAL의 code
CREATE UNIQUE INDEX IF NOT EXISTS uk_indexing_jobs_target_source_id_job_status ON indexing_jobs (target_source_id) WHERE indexing_job_type = 'MANUAL' AND job_status IN ('PENDING', 'RUNNING');
CREATE UNIQUE INDEX IF NOT EXISTS uk_indexing_jobs_target_post_id_job_status ON indexing_jobs (target_post_id) WHERE indexing_job_type = 'MANUAL' AND job_status IN ('PENDING', 'RUNNING');

-- MANUAL일 때 둘 중 정확히 하나만 채워져야 한다는 비즈니스 규칙
ALTER TABLE indexing_jobs
    ADD CONSTRAINT chk_manual_job_target
        CHECK (
            indexing_job_type != 'MANUAL' -- CRON은 검증 안 함
    OR (target_source_id IS NOT NULL AND target_post_id IS NULL)
    OR (target_source_id IS NULL AND target_post_id IS NOT NULL)
    );

CREATE TABLE collect_source_posts
(
    id                     UUID PRIMARY KEY,
    collect_source_id      UUID         NOT NULL,
    title                  VARCHAR      NOT NULL,
    url                    VARCHAR(512) NOT NULL,
    published_at           TIMESTAMP    NOT NULL,
    thumbnail_url          VARCHAR,
    summary                TEXT,
    content                TEXT,
    content_hash           VARCHAR,
    indexing_status        VARCHAR(20),
    indexing_error_count   INTEGER   DEFAULT 0,
    last_indexed_at        TIMESTAMP DEFAULT NULL,
    last_collected_at      TIMESTAMP DEFAULT NOW(),
    last_collecting_job_id UUID,
    last_indexing_job_id   UUID,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP    NOT NULL,
    deleted_at             TIMESTAMP,

    CONSTRAINT fk_collect_source_posts_collect_source_id FOREIGN KEY (collect_source_id) REFERENCES collect_sources (id),
    CONSTRAINT fk_collect_source_posts_last_collecting_job_id FOREIGN KEY (last_collecting_job_id) REFERENCES collecting_jobs (id),
    CONSTRAINT fk_collect_source_posts_last_indexing_job_id FOREIGN KEY (last_indexing_job_id) REFERENCES indexing_jobs (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_collect_source_posts_url ON collect_source_posts (url) WHERE deleted_at IS NULL;

ALTER TABLE indexing_jobs
    ADD CONSTRAINT fk_indexing_jobs_target_post_id
        FOREIGN KEY (target_post_id) REFERENCES collect_source_posts (id);

CREATE TABLE report_types
(
    code       INTEGER PRIMARY KEY,
    category   VARCHAR,
    name       VARCHAR,
    is_used    BOOLEAN   NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

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

-- 4. 사용자 활동 관련 테이블 (post, user 참조)
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
