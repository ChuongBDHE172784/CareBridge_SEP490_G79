# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -DskipTests package
FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system carebridge && useradd --system --gid carebridge --home-dir /app --shell /usr/sbin/nologin carebridge
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown carebridge:carebridge /app/app.jar
USER carebridge
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
# No application health endpoint exists yet; this only checks the JVM process.
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 CMD kill -0 1 || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
