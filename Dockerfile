# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Resolve dependencies in their own layer so source-only changes don't re-download them.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode dependency:go-offline

COPY src src
# Tests run in CI; the integration tests need Docker (Testcontainers), which isn't available here.
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode package -DskipTests \
    && cp target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted


FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --no-create-home spring
USER spring

# Copy layers from least to most frequently changing to make the most of Docker's layer cache.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
