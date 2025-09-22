# ---- Build stage ----
FROM gradle:8.10.2-jdk17 AS build


WORKDIR /app

# Copy Gradle wrapper and dependency files first for caching
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Download dependencies
RUN ./gradlew --no-daemon build -x test || true

# Copy the source code
COPY src ./src

# Build the fat jar
RUN ./gradlew --no-daemon bootJar -x test

# ---- Runtime stage ----
FROM gcr.io/distroless/java21:nonroot

WORKDIR /app

# Add OpenTelemetry Java agent
COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# Copy built jar
COPY --from=build /app/build/libs/*.jar /app/service.jar

# Default entrypoint using Java agent
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/service.jar"]


# # ---- Build stage ----
# FROM gradle:8.10.2-jdk17 AS build
# WORKDIR /app

# # Copy Gradle wrapper and dependencies first for caching
# COPY gradle gradle
# COPY gradlew .
# COPY build.gradle .
# COPY settings.gradle .

# # Pre-download dependencies
# RUN ./gradlew --no-daemon build -x test || true

# # Copy source and build Spring Boot fat jar
# COPY src ./src
# RUN ./gradlew --no-daemon bootJar -x test

# # Extract the bootJar layers
# RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted


# # ---- JDK Analysis stage ----
# FROM eclipse-temurin:17-jdk AS jre-build
# WORKDIR /jre

# # Copy the fat jar from build stage
# COPY --from=build /app/build/libs/*.jar app.jar

# # Use jdeps to find required modules for this app
# RUN jdeps \
#     --ignore-missing-deps \
#     --print-module-deps \
#     --recursive \
#     --multi-release 17 \
#     app.jar > modules.txt

# # Build a custom JRE with only needed modules
# RUN jlink \
#     --add-modules $(cat modules.txt) \
#     --strip-debug \
#     --compress=2 \
#     --no-header-files \
#     --no-man-pages \
#     --output /jre-min


# # ---- Runtime stage ----
# FROM debian:bookworm-slim AS runtime
# WORKDIR /app

# # Copy the minimal JRE
# COPY --from=jre-build /jre-min /opt/jre
# ENV PATH="/opt/jre/bin:$PATH"

# # Copy extracted Spring Boot layers for efficient caching
# COPY --from=build /app/extracted/dependencies/ ./dependencies/
# COPY --from=build /app/extracted/spring-boot-loader/ ./spring-boot-loader/
# COPY --from=build /app/extracted/snapshot-dependencies/ ./snapshot-dependencies/
# COPY --from=build /app/extracted/application/ ./application/

# # Add OpenTelemetry agent
# COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# # Run using Spring Boot layered jar layout
# ENTRYPOINT ["java", \
#     "-javaagent:/app/opentelemetry-javaagent.jar", \
#     "org.springframework.boot.loader.JarLauncher"]
