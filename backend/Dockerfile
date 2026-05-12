# === Build stage ===
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first (layer caching)
COPY pom.xml ./
RUN mvn dependency:go-offline -DskipTests --no-transfer-progress

# Build application
COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress

# === Runtime stage ===
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
