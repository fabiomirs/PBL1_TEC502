import java.io.*;
import java.net.*;

public class SimpleServer {
    public static void main(String[] args) {
        int port = 8080; // Define a porta na qual o servidor vai escutar

        // Tenta criar um ServerSocket que escuta na porta especificada
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor escutando na porta " + port);

            // Loop infinito para aceitar conexões de clientes
            while (true) {
                try {
                    // Aceita uma conexão do cliente e cria um Socket para comunicação
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

                    // Inicia uma thread para tratar a comunicação com o cliente
                    AtendeCliente ac = new AtendeCliente(clientSocket);
                    ac.start(); // Inicia a thread para tratar a comunicação com o cliente

                } catch (IOException e) {
                    // Trata erros que podem ocorrer ao aceitar uma conexão
                    System.out.println("Erro ao aceitar conexão: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // Trata erros que podem ocorrer ao iniciar o servidor
            System.out.println("Erro ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
