# Estágio 1: Build com Maven
# Este estágio apenas compila o seu código e gera o ficheiro .jar
FROM eclipse-temurin:17-jdk-focal as builder
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN ./mvnw install -DskipTests

# Estágio 2: Imagem de Produção Final
# Começamos com uma imagem base limpa
FROM eclipse-temurin:17-jre-focal
WORKDIR /workspace/app

# --- CORREÇÃO IMPORTANTE: Instalação de Fontes no Estágio Final ---
# Atualiza os pacotes e instala o fontconfig, necessário para gerir fontes no sistema.
RUN apt-get update && apt-get install -y fontconfig

# Copia os seus ficheiros de fonte para a pasta de fontes do sistema.
# Note que estamos a copiar do contexto do build, não do estágio 'builder'.
COPY fonts/ /usr/share/fonts/truetype/custom/

# Atualiza o cache de fontes do sistema para que a JVM as encontre.
RUN fc-cache -f -v
# --- FIM DA CORREÇÃO ---

# Copia APENAS o JAR construído no estágio anterior
# O Spring Boot Maven Plugin gera o JAR com o nome do artifactId
COPY --from=builder /workspace/app/target/app.jar app.jar

# Expõe a porta da aplicação
EXPOSE 8081

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
