package usandoNIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClienteNio {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        // Estabelece a conexão com o servidor
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 12345));
        socketChannel.configureBlocking(false);  // Manter não-bloqueante

        System.out.println("Conectado ao servidor de passagens!");

        // Solicitar o CPF para identificação com validação
        String cpf;
        boolean cpfValido = false;  // Adiciona uma flag para controlar a validade do CPF

        while (!cpfValido) {
            System.out.print("Digite seu CPF (11 dígitos): ");
            cpf = scanner.nextLine();

            // Verifica se o CPF contém apenas números e possui 11 dígitos
            if (cpf.matches("\\d{11}")) {
                // Enviar o CPF ao servidor
                enviarMensagem(socketChannel, cpf);

                // Aguardar a resposta do servidor sobre a validade do CPF
                String respostaServidor = receberResposta(socketChannel);

                if (respostaServidor.contains("CPF registrado")) {
                    cpfValido = true;  // CPF aceito pelo servidor, sair do loop
                } else if (respostaServidor.contains("CPF já em uso")) {
                    System.out.println("Tente novamente.");
                }
            } else {
                System.out.println("CPF inválido. O CPF deve conter exatamente 11 dígitos numéricos.");
            }
        }

        // Loop de interação com o cliente após CPF válido
        while (true) {
            String opcao = menu();

            if (opcao.equals("1")) {
                String origem_cliente = local_cliente();
                String mensagem = "comprar," + origem_cliente;
                enviarMensagem(socketChannel, mensagem);
                receberResposta(socketChannel);
                
                System.out.print("Para onde deseja ir: ");

                Scanner scanner2 = new Scanner(System.in);
                String destino_cliente = scanner2.nextLine();

                mensagem = "comprar," + destino_cliente+","+"2";
                enviarMensagem(socketChannel, mensagem);

            } else if (opcao.equals("2")) {
                String mensagem = "listar,";
                enviarMensagem(socketChannel, mensagem);

            } else if (opcao.equals("3")) {
                String mensagem = "sair,";
                enviarMensagem(socketChannel, mensagem);  // Enviar mensagem de saída ao servidor
                socketChannel.close();  // Fecha a conexão
                System.out.println("Conexão encerrada.");
                break;  
            }

            // Receber resposta do servidor
            receberResposta(socketChannel);
        }
    }

    private static void enviarMensagem(SocketChannel socketChannel, String mensagem) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(mensagem.getBytes());
        socketChannel.write(buffer);
        buffer.clear();
    }

    private static String receberResposta(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int bytesRead = 0;

        // Loop para aguardar a resposta do servidor
        while (bytesRead == 0) {
            bytesRead = socketChannel.read(buffer);
        }

        if (bytesRead > 0) {
            buffer.flip();
            String resposta = new String(buffer.array(), 0, bytesRead);
            System.out.println("Resposta do servidor: ");
            System.out.println(resposta);
            return resposta;
        }

        return "";  // Retorna string vazia em caso de falha
    }


    public static String menu(){
        System.out.println("Escolha uma ação: ");
        System.out.println("[1] Comprar passagem");
        System.out.println("[2] Listar trechos disponíveis");
        System.out.println("[3] Sair");

        System.out.print("Digite sua opcao: ");
        Scanner scanner = new Scanner(System.in);
        String mensagem = scanner.nextLine();


        return mensagem;

    }

    public static String local_cliente(){
        System.out.print("Digite qual cidade voce está: ");
        Scanner scanner = new Scanner(System.in);
        String origem_cliente = scanner.nextLine();
        String compactar_origem_cliente = origem_cliente+","+"1";
        return compactar_origem_cliente;
    }

    
}
