FROM debian:bookworm-slim AS ticketverify-engine
ARG TICKETVERIFY_ENGINE_REPO=https://github.com/nico8156/ticket_engine.git
ARG TICKETVERIFY_ENGINE_REF=cdebb4e33cc419f5111a2a93b9a4f4f82e1b2bb5

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates cmake g++ git make \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /ticketverify-engine
RUN git init -b build . \
    && git remote add origin "${TICKETVERIFY_ENGINE_REPO}" \
    && git fetch --depth 1 origin "${TICKETVERIFY_ENGINE_REF}" \
    && git checkout FETCH_HEAD \
    && sed -i '/include(CTest)/d;/enable_testing()/d;/add_subdirectory(tests)/d' CMakeLists.txt \
    && cmake -S . -B build -DCMAKE_BUILD_TYPE=Release \
    && cmake --build build --target ticketverify --parallel

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
COPY --from=ticketverify-engine /ticketverify-engine/build/ticketverify ./bin/ticketverify

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/app.jar
COPY --from=build /workspace/bin/ticketverify /app/bin/ticketverify

RUN chmod +x /app/bin/ticketverify

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
