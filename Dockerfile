FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress verify

FROM mcr.microsoft.com/playwright/java:v1.62.0-noble

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Sao_Paulo \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

COPY --from=build --chown=pwuser:pwuser \
    /workspace/target/judicial-pipeline-0.0.1-SNAPSHOT-exec.jar \
    /app/judicial-pipeline.jar

USER pwuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/judicial-pipeline.jar"]
