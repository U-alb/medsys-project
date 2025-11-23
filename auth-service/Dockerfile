# Simple runtime image, assumes JAR is already built in target/
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the built jar (any name) to app.jar
COPY target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
