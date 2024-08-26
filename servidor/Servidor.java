package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Servidor {
    // Map para armazenar os trechos disponíveis e suas respectivas quantidades de assentos
    private static Map<String, Integer> trechosDisponiveis = new HashMap<>();

    public static void main(String[] args) throws IOException {
        // Inicializa os trechos disponíveis e suas quantidades de assentos
        inicializarTrechos();

        // Cria um ServerSocket na porta 4000
        ServerSocket serverSocket = new ServerSocket(4000);
        System.out.println("servidor.Servidor iniciado e aguardando conexões...");

        while (true) {
            // Espera até que um cliente se conecte ao servidor
            Socket socket = serverSocket.accept();
            System.out.println("cliente.Cliente conectado: " + socket.getInetAddress());

            // Cria uma nova thread para lidar com a comunicação com este cliente
            ClientHandler clientHandler = new ClientHandler(socket);
            new Thread(clientHandler).start();
        }
    }

    // Inicializa os trechos disponíveis com quantidades de assentos
    private static void inicializarTrechos() {
        trechosDisponiveis.put("Belém-Fortaleza", 50);
        trechosDisponiveis.put("Fortaleza-São Paulo", 30);
        trechosDisponiveis.put("São Paulo-Curitiba", 20);
        trechosDisponiveis.put("Rio de Janeiro-Belo Horizonte", 40);
        trechosDisponiveis.put("Salvador-Recife", 25);
        // Adicione outros trechos conforme necessário
    }

    // Classe que lida com a comunicação com o cliente em uma thread separada
    static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                InputStreamReader inputReader = new InputStreamReader(socket.getInputStream());
                BufferedReader reader = new BufferedReader(inputReader);
                PrintStream saida = new PrintStream(socket.getOutputStream());

                String mensagemCliente;

                while ((mensagemCliente = reader.readLine()) != null) {
                    System.out.println("cliente.Cliente mandou uma mensagem: " + mensagemCliente);

                    if (mensagemCliente.startsWith("RESERVAR")) {
                        String trecho = mensagemCliente.split(" ")[1];
                        int quantidade = Integer.parseInt(mensagemCliente.split(" ")[2]);

                        if (trechosDisponiveis.containsKey(trecho) && trechosDisponiveis.get(trecho) >= quantidade) {
                            // Reduz a quantidade de assentos disponíveis no trecho
                            trechosDisponiveis.put(trecho, trechosDisponiveis.get(trecho) - quantidade);
                            saida.println("Reserva confirmada para " + quantidade + " assento(s) no trecho: " + trecho);
                        } else {
                            saida.println("Trecho indisponível ou quantidade solicitada indisponível para: " + trecho);
                        }
                    } else if (mensagemCliente.equals("LISTAR TRECHOS")) {
                        // Lista todos os trechos disponíveis e suas quantidades de assentos
                        StringBuilder resposta = new StringBuilder("Trechos disponíveis:\n");
                        for (Map.Entry<String, Integer> entry : trechosDisponiveis.entrySet()) {
                            resposta.append(entry.getKey()).append(" - Assentos disponíveis: ").append(entry.getValue()).append("\n");
                        }
                        saida.println(resposta.toString());
                    } else {
                        saida.println("Comando não reconhecido.");
                    }
                }

                // Fecha o socket do cliente após a comunicação terminar
                socket.close();
                System.out.println("cliente.Cliente desconectado: " + socket.getInetAddress());

            } catch (IOException e) {
                System.err.println("Erro na comunicação com o cliente: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
