FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/trade-executor-0.1.0.jar /app/trade-executor.jar
CMD ["java", "-jar", "/app/trade-executor.jar"]
