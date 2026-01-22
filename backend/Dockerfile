# ---- Build stage ----
# Use a Debian-based builder (not Alpine) to avoid native-lib issues (e.g., netty tcnative on musl).
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy everything (adjust if your repo layout is different)
COPY . .

# Build the Spring Boot jar (your earlier Dockerfile built in /workspace/backend)
RUN cd backend && mvn -DskipTests clean package


# ---- Runtime stage ----
# Use a Debian-based JRE image (not Alpine) for better native library compatibility.
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar (matches your backend/target output)
COPY --from=builder /workspace/backend/target/*.jar /app/app.jar

# Render provides $PORT; Spring should bind to it (we set SERVER_PORT to be safe).
# If your application already has server.port=${PORT:8080}, this is optional but harmless.
ENV SERVER_PORT=${PORT}

# Optional: helps avoid Netty OpenSSL/tcnative crashes by forcing JDK SSL provider.
# You can also set this in Render as JAVA_TOOL_OPTIONS instead of baking into the image.
ENV JAVA_TOOL_OPTIONS="-Dio.netty.handler.ssl.noOpenSsl=true"

# Render doesn't require EXPOSE, but it doesn't hurt (defaults to 8080 locally)
EXPOSE 8080

# IMPORTANT: JVM args must come BEFORE -jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
