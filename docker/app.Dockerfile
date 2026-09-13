# syntax=docker/dockerfile:1
# 사용: docker build -f docker/app.Dockerfile --build-arg MODULE=user-service .
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN chmod +x gradlew && ./gradlew ":${MODULE}:bootJar" --no-daemon -x test

FROM eclipse-temurin:17-jre-jammy
# compose prod 의 user: "1001:1001" 과 맞춤 — 파일 로그(./logs) 쓰기용
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid 1001 --home-dir /app --no-create-home app
WORKDIR /app
ARG MODULE
COPY --from=build --chown=1001:1001 /src/${MODULE}/build/libs/*.jar /app/app.jar
RUN mkdir -p /app/logs && chown 1001:1001 /app/logs
USER 1001
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
