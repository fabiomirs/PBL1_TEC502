# Etapa de construção (compilação do código Java)
FROM openjdk:11 AS build
WORKDIR /app
COPY . /app
RUN javac servidor/Servidor.java cliente/Cliente.java

# Etapa de execução do servidor
FROM openjdk:11 AS servidor
WORKDIR /app
COPY --from=build /app /app
CMD ["java", "servidor.Servidor"]

# Etapa de execução do cliente
FROM openjdk:11 AS cliente
WORKDIR /app
COPY --from=build /app /app
CMD ["java", "cliente.Cliente"]
