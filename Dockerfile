# Stage 1 — build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src

# instala Maven no container
RUN apk add --no-cache maven && mvn clean package -DskipTests

# Stage 2 — runtime (imagem mínima)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# JGit precisa de git instalado
RUN apk add --no-cache git

COPY --from=builder /app/target/arq-analyzer-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]