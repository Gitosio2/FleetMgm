FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
# mvnw is checked out with CRLF line endings + a possibly-lost exec bit on a Windows host
# (git tracks it as 100755, but that's not always what lands on disk here) — normalize both
# so the shebang resolves inside this Linux image regardless of how the build context was checked out.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# curl is needed by docker-compose's healthcheck (`/actuator/health`) — not present in this base image by default
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
