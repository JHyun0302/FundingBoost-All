FROM gradle:8.7.0-jdk17 AS builder

WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src src

RUN gradle clean bootJar -x test -x asciidoctor --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
