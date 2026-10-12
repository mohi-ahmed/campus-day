FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -ntp dependency:go-offline

COPY lombok.config ./
COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package \
    && cp target/*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S campusday \
    && adduser -S -G campusday campusday

WORKDIR /app
COPY --from=build --chown=campusday:campusday /workspace/app.jar ./app.jar

USER campusday

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
