FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]