
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre AS runtime

ARG SERVICE=dashboard-service
WORKDIR /app

RUN test -n "$SERVICE" || (echo "ERROR: Please provide --build-arg SERVICE=<service-name>" && exit 1)

COPY --from=builder /workspace/${SERVICE}/target/${SERVICE}-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
