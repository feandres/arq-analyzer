FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# copia o maven wrapper primeiro
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# baixa dependências em camada separada (cache)
RUN ./mvnw dependency:go-offline -q

# copia o código e compila
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache git
COPY --from=builder /app/target/arq-analyzer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]