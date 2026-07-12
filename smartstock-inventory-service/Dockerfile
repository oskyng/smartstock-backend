FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
COPY src/main/resources/Wallet_SmartStock /app/wallet

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app-ms.jar
COPY --from=build /app/wallet /app/wallet

ENV TNS_ADMIN=/app/wallet

ENTRYPOINT ["java", "-jar", "app-ms.jar"]