
FROM eclipse-temurin:17-jdk-focal as builder
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw install -DskipTests

# Estágio 2: Imagem de Produção
FROM eclipse-temurin:17-jre-focal
WORKDIR /workspace/app

# Copia o JAR construído no estágio anterior
COPY --from=builder /workspace/app/target/*.jar app.jar

# Expõe a porta da aplicação
EXPOSE 8081

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
