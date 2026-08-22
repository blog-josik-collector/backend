# syntax=docker/dockerfile:1
# 사용: docker build -f docker/app.Dockerfile --build-arg MODULE=user-service .
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN chmod +x gradlew && ./gradlew ":${MODULE}:bootJar" --no-daemon -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG MODULE
COPY --from=build /src/${MODULE}/build/libs/*.jar /app/app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
