INSERT INTO post_providers (id, name, description, base_url, is_used, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'kakao', '카카오 기술 블로그', 'https://tech.kakao.com/blog', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'line', '라인 기술 블로그', 'https://techblog.lycorp.co.jp/ko', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'toss', '토스 기술 블로그', 'https://toss.tech', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
