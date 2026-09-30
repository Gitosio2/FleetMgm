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

# Pin every memory region instead of letting the JVM derive them from the container limit. Railway
# bills actual memory usage, and the plan's RAM ceiling is not stable: the Free plan caps a service
# at 0.5 GB, Trial at 1 GB and Hobby at 8 GB — so derived defaults would silently grow the heap, and
# the bill, on a plan change alone.
#
# Measured, not guessed: booting this image's jar against a demo-seeded PostgreSQL 16 and driving
# 209 requests (three passes over 50 invoice PDFs plus list endpoints, then 6 PDFs in parallel),
# the previous flags (-Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m) idled at 485 MB and
# peaked at 527 MB RSS — over the Free plan's 512 MB cap. The flags below idle at 357 MB and peak at
# 386 MB, with no errors and a heap that still has ~100 MB of headroom (live set ~58 MB of 154 MB).
#
# - SerialGC over the default G1: G1 runs concurrent collector threads and reserves region
#   bookkeeping that only pay off on heaps far larger than this one.
# - TieredStopAtLevel=1 + CICompilerCount=1: C1 only. The optimizing compiler's threads and code
#   are CPU and resident memory this app never earns back at demo scale.
# - MaxMetaspaceSize=128m caps the one area -Xmx does not cover. It is NOT safely lowerable: 96m
#   crashed startup with "OutOfMemoryError: Metaspace". MetaspaceSize=96m raises the initial
#   collection threshold so startup does not pay three Full GCs while metaspace grows.
# - ReservedCodeCacheSize=48m: 32m forced repeated "CodeCache GC Threshold" Full GCs.
# - Xmx160m: the workload above never needed more. A workload with far larger data than the demo
#   seed should raise it, and the Railway memory limit with it.
ENTRYPOINT ["java", "-Xmx160m", "-XX:+UseSerialGC", "-XX:MetaspaceSize=96m", "-XX:MaxMetaspaceSize=128m", "-XX:ReservedCodeCacheSize=48m", "-XX:TieredStopAtLevel=1", "-XX:CICompilerCount=1", "-jar", "app.jar"]
