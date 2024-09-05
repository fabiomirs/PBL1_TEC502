package usandoNIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClienteNio {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 12345));
        socketChannel.configureBlocking(false);  // Manter não-bloqueante

        System.out.println("Conectado ao servidor de passagens!");

        while (true) {
            System.out.println("Escolha uma ação: ");
            System.out.println("1. Comprar passagem");
            System.out.println("2. Listar trechos disponíveis");
            System.out.print("Opção: ");
            int opcao = scanner.nextInt();
            scanner.nextLine(); // Limpar o buffer

            if (opcao == 1) {
                System.out.print("Digite o trecho (ex: Belem-Fortaleza): ");
                String trecho = scanner.nextLine();
                String mensagem = "comprar," + trecho;
                enviarMensagem(socketChannel, mensagem);

            } else if (opcao == 2) {
                String mensagem = "listar,";
                enviarMensagem(socketChannel, mensagem);
            }

            // Receber resposta
            receberResposta(socketChannel);
        }
    }

    private static void enviarMensagem(SocketChannel socketChannel, String mensagem) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(mensagem.getBytes());
        socketChannel.write(buffer);
        buffer.clear();
    }

    private static void receberResposta(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int bytesRead = 0;
        while (bytesRead == 0) {  // Aguardar até receber dados
            bytesRead = socketChannel.read(buffer);
        }

        if (bytesRead > 0) {
            buffer.flip();
            String resposta = new String(buffer.array(), 0, bytesRead);
            System.out.println("Resposta do servidor: ");
            System.out.println(resposta);
        }
    }

    
}
