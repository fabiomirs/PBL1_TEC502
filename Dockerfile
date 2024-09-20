# Dockerfile

# Imagem base para rodar Java
FROM openjdk:11-jdk-slim

# Definir o diretório de trabalho
WORKDIR /app

# Copiar o arquivo POM e o código fonte
COPY demo/pom.xml .
COPY demo/src ./src
COPY dados/cidades.json /app/dados/cidades.json

# Instalar o Maven
RUN apt-get update && apt-get install -y maven

# Compilar o projeto com o Maven e criar o JAR
RUN mvn clean package

# Variável de ambiente para escolher entre cliente e servidor
ARG APP_TYPE=server

# Expor a porta do servidor, se necessário
EXPOSE 12345

# Comando para rodar o cliente ou servidor
CMD ["sh", "-c", "java -cp target/demo-1.0-SNAPSHOT-jar-with-dependencies.jar usandoNIO.${APP_TYPE}"]
