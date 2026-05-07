# Build stage
FROM amazoncorretto:25-alpine-jdk AS builder
WORKDIR /app
COPY . .
RUN apk add --no-cache bash && \
    chmod +x gradlew && \
    ./gradlew clean bootJar -x test

# Runtime stage
FROM amazoncorretto:25-alpine-jdk
VOLUME /tmp
COPY --from=builder /app/build/libs/application.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]