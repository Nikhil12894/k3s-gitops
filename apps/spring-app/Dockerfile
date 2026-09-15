## Stage 1: Build the Spring Boot 4.1.0 JAR using Gradle & Java 25
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle .
COPY src src

RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

## Stage 2: Extract Spring Boot layered / exploded layout
FROM eclipse-temurin:25-jdk AS extractor
WORKDIR /workspace
COPY --from=builder /workspace/build/libs/spring-app-1.0.0.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

## Stage 3: Layered runtime image (OTel Java Agent auto-injected by K8s OTel Operator)
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy layers in order of least-frequent to most-frequent changes for optimal Docker caching
COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
