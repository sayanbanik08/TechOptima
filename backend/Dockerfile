# Multi-stage build for TechOptima
FROM maven:3.8.8-eclipse-temurin-11 AS build
WORKDIR /app
COPY backend/pom.xml backend/
COPY backend/src backend/src/
RUN mvn -f backend/pom.xml clean package -DskipTests

# Production Runtime Stage
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/TechOptima-1.0-SNAPSHOT.jar app.jar
ENV TECHOPTIMA_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

