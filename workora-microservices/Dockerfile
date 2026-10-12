
FROM maven:3.9-eclipse-temurin-17 AS builder

ARG SERVICE=dashboard-service
WORKDIR /workspace
COPY . .
RUN mvn -B -DskipTests package \
    && service="${SERVICE:-dashboard-service}" \
    && jar="/workspace/${service}/target/${service}-1.0.0.jar" \
    && if [ ! -f "$jar" ]; then echo "ERROR: No packaged jar found for SERVICE=${service}"; exit 1; fi \
    && cp "$jar" /workspace/service.jar

FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

COPY --from=builder /workspace/service.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
