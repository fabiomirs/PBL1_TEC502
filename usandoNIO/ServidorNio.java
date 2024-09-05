package usandoNIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorNio {
    private static ConcurrentHashMap<String, Integer> trechosDisponiveis = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        // Inicializar os trechos e passagens disponíveis
        trechosDisponiveis.put("Belem-Fortaleza", 10);
        trechosDisponiveis.put("Fortaleza-SaoPaulo", 8);
        trechosDisponiveis.put("SaoPaulo-Curitiba", 5);

        // Configuração do NIO
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(12345));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Servidor pronto para receber conexões...");

        while (true) {
            selector.select(); // Espera por eventos
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                if (key.isAcceptable()) {
                    // Aceitar nova conexão
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("Novo cliente conectado: " + clientChannel.getRemoteAddress());

                } else if (key.isReadable()) {
                    // Ler dados do cliente
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(256);
                    int bytesRead = clientChannel.read(buffer);

                    if (bytesRead == -1) {
                        clientChannel.close();
                    } else {
                        buffer.flip();
                        String request = new String(buffer.array()).trim();
                        System.out.println("Recebido: " + request);

                        String[] parts = request.split(",");
                        String acao = parts[0];

                        if (acao.equals("comprar")) {
                            String trecho = parts[1];
                            processarCompra(clientChannel, trecho);
                        } else if (acao.equals("listar")) {
                            listarTrechos(clientChannel);
                        }
                    }
                }
                iterator.remove();
            }
        }
    }

    private static synchronized void processarCompra(SocketChannel clientChannel, String trecho) throws IOException {
        if (trechosDisponiveis.containsKey(trecho)) {
            int passagensRestantes = trechosDisponiveis.get(trecho);

            if (passagensRestantes > 0) {
                trechosDisponiveis.put(trecho, passagensRestantes - 1);
                String resposta = "Passagem comprada para o trecho " + trecho + ". Restantes: " + (passagensRestantes - 1);
                clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
            } else {
                String resposta = "Desculpe, não há mais passagens disponíveis para o trecho " + trecho;
                clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
            }
        } else {
            String resposta = "Trecho inválido!";
            clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
        }
    }

    private static void listarTrechos(SocketChannel clientChannel) throws IOException {
        StringBuilder response = new StringBuilder("Trechos disponíveis:\n");
        trechosDisponiveis.forEach((trecho, quantidade) -> {
            response.append(trecho).append(": ").append(quantidade).append(" passagens restantes\n");
        });

        clientChannel.write(ByteBuffer.wrap(response.toString().getBytes()));
    }
}
