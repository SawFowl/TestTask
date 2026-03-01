# Сборка
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon -x test

# Запуск
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar testtask.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "testtask.jar"]
