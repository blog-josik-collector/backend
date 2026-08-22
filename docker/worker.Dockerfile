# syntax=docker/dockerfile:1
# Selenium 크롤용 Chromium이 포함된 integrated-worker 이미지
# Ubuntu Jammy는 chromium apt가 snap 래퍼라 Docker에서 실패 → Debian bookworm 사용
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew :integrated-worker:bootJar --no-daemon -x test

FROM debian:bookworm-slim
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-17-jre-headless \
        chromium \
        chromium-driver \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /src/integrated-worker/build/libs/*.jar /app/app.jar
ENV JAVA_OPTS=""
ENV CHROME_BINARY=/usr/bin/chromium
ENV CHROME_DRIVER=/usr/bin/chromedriver
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
