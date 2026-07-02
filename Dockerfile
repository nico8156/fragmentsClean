FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
COPY bin ./bin

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/app.jar
COPY --from=build /workspace/bin/ticketverify /app/bin/ticketverify

RUN chmod +x /app/bin/ticketverify

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
