# Dockerfile para backend Spring Boot (usando JAR precompilado)

# Imagen base con JDK 21
FROM eclipse-temurin:21-jdk

# Carpeta de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el JAR compilado desde tu laptop al contenedor
COPY build/libs/*.jar app.jar

# Exponemos el puerto que tu aplicación usa
EXPOSE 1200

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
