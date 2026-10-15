FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/medisense-*.jar app.jar
RUN addgroup -S medisense && adduser -S medisense -G medisense
USER medisense
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
