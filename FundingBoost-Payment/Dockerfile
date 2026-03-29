FROM gradle:8.7.0-jdk17 AS builder
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src src
RUN gradle clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/fundingboost-payment
COPY --from=builder /workspace/build/libs/fundingboost-payment.jar fundingboost-payment.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar fundingboost-payment.jar"]
