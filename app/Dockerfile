# ---------- Build stage ----------
FROM gradle:8.7-jdk21 AS builder

ARG EXCHANGE_FILL_WORKER_ENABLED=true
ENV EXCHANGE_FILL_WORKER_ENABLED=$EXCHANGE_FILL_WORKER_ENABLED

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle clean test bootJar --no-daemon --info

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
