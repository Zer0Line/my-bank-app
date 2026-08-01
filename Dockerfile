FROM eclipse-temurin:21-jre
ARG JAR_FILE
ARG APP_PORT=8080
COPY ${JAR_FILE} /app/app.jar
EXPOSE ${APP_PORT}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
