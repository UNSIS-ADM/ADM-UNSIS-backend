# Etapa 1: Compilar JAR
FROM gradle:8.4-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew --no-daemon clean build --refresh-dependencies



# Etapa 2: Imagen final para correr la app
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 1200
ENTRYPOINT ["java", "-jar", "app.jar"]
