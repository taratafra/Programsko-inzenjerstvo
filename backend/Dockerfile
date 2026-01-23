
# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy repo into build container
COPY . .

# Build the Spring Boot jar (adjust if your jar name differs)
RUN cd backend && mvn -DskipTests package


# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar
COPY --from=builder /workspace/backend/target/*.jar /app/app.jar

COPY --from=builder /workspace/backend/ContentBase/ /app/ContentBase/

# Render sets PORT; Spring should bind to it
ENV SERVER_PORT=${PORT}

ENV FIREBASE_CREDENTIALS_PATH=/etc/secrets/serviceAccountKey.json

# Helpful JVM option (safe to keep)
ENV JAVA_TOOL_OPTIONS="-Dio.netty.handler.ssl.noOpenSsl=true"

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
