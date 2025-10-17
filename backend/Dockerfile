# 1) Build stage (Java 21)
FROM eclipse-temurin:21-alpine AS builder

WORKDIR /workspace

# Copy everything from backend/ (Dockerfile context is already backend/)
COPY . .
RUN chmod +x ./backend/mvnw

# Build your Spring Boot JAR
RUN ./backend/mvnw clean package 

# 2) Runtime stage (Java 21 JRE)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the packaged JAR from the builder stage
COPY --from=builder /workspace/target/*.jar /app/app.jar

# Expose the default Spring Boot port (optional)
EXPOSE 8080

# Run the backend
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
