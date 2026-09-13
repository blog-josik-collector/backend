# syntax=docker/dockerfile:1
# Selenium 크롤용 Chromium이 포함된 integrated-worker 이미지
# Ubuntu Jammy는 chromium apt가 snap 래퍼라 Docker에서 실패 → Debian bookworm 사용
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew :integrated-worker:bootJar --no-daemon -x test

FROM debian:bookworm-slim
# compose prod 의 user: "1001:1001" 과 맞춤 — 파일 로그(./logs) 쓰기용
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid 1001 --home-dir /app --no-create-home app
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-17-jre-headless \
        chromium \
        chromium-driver \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build --chown=1001:1001 /src/integrated-worker/build/libs/*.jar /app/app.jar
RUN mkdir -p /app/logs && chown 1001:1001 /app/logs
USER 1001
ENV HOME=/tmp
ENV JAVA_OPTS=""
ENV CHROME_BINARY=/usr/bin/chromium
ENV CHROME_DRIVER=/usr/bin/chromedriver
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
