# ====================================
# Stage 1: Build the application
# ====================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (for better layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (cached layer - only re-runs if pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN ./mvnw package -DskipTests -B

# ====================================
# Stage 2: Run the application
# ====================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create uploads directory
RUN mkdir -p /app/uploads /app/logs && chown -R spring:spring /app

# Copy the built JAR from the builder stage
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check will be managed by Render dashboard configuration

# Run the application with production profile
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:TieredStopAtLevel=1", \
    "-noverify", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
