FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
VOLUME /var/copy-paste/uploads
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
