package usandoNIO;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
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
            try{
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
            catch(Exception e){
                erro_comunicacao();
            }
            
        }

        // Loop de interação com o cliente após CPF válido
        if(cpfValido){
            while (true) {
                String opcao = menu();
            
                if (opcao.equals("1")) {
                    // Etapa 1: Solicitar as cidades disponíveis para escolha de origem e destino
                    iniciarCompra(socketChannel);
    
                } else if (opcao.equals("2")) {
                    String mensagem = "sair,";
                    enviarMensagem(socketChannel, mensagem);  // Enviar mensagem de saída ao servidor
                    socketChannel.close();  // Fecha a conexão
                    System.out.println("Conexão encerrada.");
                    break;
                }
            }
        }
        
    }

    private static void iniciarCompra(SocketChannel socketChannel) throws IOException {
        // Solicitar ao servidor as cidades disponíveis
        enviarMensagem(socketChannel, "iniciar_compra,");
        String respostaCidades = receberResposta(socketChannel);

        // Mapeamento das cidades recebidas (número -> nome da cidade)
        Map<Integer, String> cidadesMap = new HashMap<>();
        String[] linhas = respostaCidades.split("\n");
        for (String linha : linhas) {
            if (linha.matches("\\d+\\.\\s.+")) { 
                String[] partes = linha.split("\\.\\s");
                int numero = Integer.parseInt(partes[0]);
                String nomeCidade = partes[1];
                cidadesMap.put(numero, nomeCidade);
            }
        }

        try{
            // Cliente escolhe origem
            Scanner scanner = new Scanner(System.in);
            System.out.print("Digite o número correspondente à cidade de origem: ");
            int numOrigem = Integer.parseInt(scanner.nextLine());
            String cidadeOrigem = cidadesMap.get(numOrigem);

            // Cliente escolhe destino
            System.out.print("Digite o número correspondente à cidade de destino: ");
            int numDestino = Integer.parseInt(scanner.nextLine());
            String cidadeDestino = cidadesMap.get(numDestino);

            // Enviar as cidades escolhidas ao servidor para listar rotas
            String mensagemEscolherCidades = "escolher_cidades," + cidadeOrigem + "," + cidadeDestino;
            enviarMensagem(socketChannel, mensagemEscolherCidades);

            String respostaRotas = receberResposta(socketChannel);

            // Cliente escolhe uma rota
            System.out.print("Digite o número correspondente à rota escolhida: ");
            String rotaEscolhida = scanner.nextLine();

            String mensagemEscolherRota = "escolher_rota," + rotaEscolhida + "," + cidadeDestino;
            enviarMensagem(socketChannel, mensagemEscolherRota);

            // Receber confirmação da compra
            String respostaCompra = receberResposta(socketChannel);
        }
        catch(Exception e){
            erro_comunicacao();
        }
        
    }

    private static void enviarMensagem(SocketChannel socketChannel, String mensagem) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(mensagem.getBytes());
        socketChannel.write(buffer);
        buffer.clear();
    }

    private static String receberResposta(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead = 0;

        // Loop para aguardar a resposta do servidor
        while (bytesRead == 0) {
            bytesRead = socketChannel.read(buffer);
        }

        if (bytesRead > 0) {
            buffer.flip();
            System.out.print("\033[H\033[2J");
            System.out.flush();
            String resposta = new String(buffer.array(), 0, bytesRead);
            System.out.println("Resposta do servidor: ");
            System.out.println(resposta);
            return resposta;
        }

        return "";  
    }

    public static String menu() {
        try{
            System.out.println("Escolha uma ação: \n");
            System.out.println("[1] Comprar passagem");
            System.out.println("[2] Sair");
        
            System.out.print("Digite sua opcao: ");
            Scanner scanner = new Scanner(System.in);
            String mensagem = scanner.nextLine();
        
            return mensagem;
        }
        catch(Exception e){
            erro_comunicacao();
            return null;
        }
    }


    public static void erro_comunicacao(){
        System.err.println("ERRO de comunicação: Conexão interrompida bruscamente!");
        System.exit(0);
        
    }
}