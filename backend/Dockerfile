FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Gradle wrapper and build files first for dependency layer caching
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY server/build.gradle.kts server/

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source and build the fat jar
COPY server/src/ server/src/
RUN ./gradlew :server:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/server/build/libs/server-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
