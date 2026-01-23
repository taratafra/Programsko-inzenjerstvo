# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy the whole repo into the build container
COPY . .

# Build the Spring Boot jar
RUN cd backend && mvn -DskipTests package


# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar
COPY --from=builder /workspace/backend/target/*.jar /app/app.jar

# Copy ContentBase so your seeding initializer can find it
# If ContentBase is NOT under backend/, change to:
# COPY --from=builder /workspace/ContentBase/ /app/ContentBase/
COPY --from=builder /workspace/backend/ContentBase/ /app/ContentBase/

ENV JAVA_TOOL_OPTIONS="-Dio.netty.handler.ssl.noOpenSsl=true"
EXPOSE 8080

# At runtime (on Render), copy the mounted secret file into /app,
# then start Spring Boot.
ENTRYPOINT ["/bin/sh", "-lc", "\
  if [ -f /etc/secrets/serviceAccountKey.json ]; then \
    echo '[entrypoint] Copying Firebase key from /etc/secrets/serviceAccountKey.json to /app/serviceAccountKey.json'; \
    cp /etc/secrets/serviceAccountKey.json /app/serviceAccountKey.json; \
    chmod 400 /app/serviceAccountKey.json; \
  else \
    echo '[entrypoint] Firebase key not found at /etc/secrets/serviceAccountKey.json'; \
  fi; \
  exec java -Dspring.profiles.active=prod -jar /app/app.jar \
"]
