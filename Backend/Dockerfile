# Imagen del backend AegisOPS (multi-etapa)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S aegis && adduser -S aegis -G aegis
COPY --from=build /app/target/*.jar app.jar
USER aegis
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
