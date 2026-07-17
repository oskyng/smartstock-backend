FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
COPY src/main/resources/Wallet_SmartStock /app/wallet

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN addgroup --system smartstock && adduser --system --ingroup smartstock smartstock

WORKDIR /app

COPY --from=build --chown=smartstock:smartstock /app/target/*.jar app-ms.jar
COPY --from=build --chown=smartstock:smartstock /app/wallet /app/wallet

ENV TNS_ADMIN=/app/wallet

USER smartstock

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "app-ms.jar"]