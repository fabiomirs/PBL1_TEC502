package usandoNIO;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/*
 * Classe que representa o cliente na comunicação cliente-servidor e possui todas as interações realizadas
 * na comunicação.
 */
public class ClienteNio {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        // Estabelece a conexão com o servidor
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("servidor", 12345));
        // Mantém o canal como não-bloqueante
        socketChannel.configureBlocking(false);  

        System.out.println("Conectado ao servidor de passagens!");

        // Solicitar o CPF para identificação com validação
        String cpf;
        // Flag para controlar a validade do CPF a partir da resposta do servidor
        boolean cpfValido = false;  

        while (!cpfValido) {
            try{
                System.out.print("Digite seu CPF (11 dígitos): ");
                cpf = scanner.nextLine();

                // Verifica se o CPF contém apenas números e possui 11 dígitos
                 if (cpf.matches("\\d{11}")) {
                    // Enviar o CPF ao servidor para realizar verificação
                    enviarMensagem(socketChannel, cpf);
                    // Aguardar a resposta do servidor sobre a validade do CPF
                    String respostaServidor = receberResposta(socketChannel);

                    if (respostaServidor.contains("CPF registrado")) {
                        // Representa que o CPF foi aceito pelo servidor
                        cpfValido = true;  
                    } else if (respostaServidor.contains("CPF já em uso")) {
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                        System.out.println("Tente novamente.");
                    }
                } else {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
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
                    // Solicitar as cidades disponíveis para escolha de origem e destino
                    iniciarCompra(socketChannel);
    
                } else if (opcao.equals("2")) {
                    String mensagem = "sair,";
                    // Enviar mensagem de saída ao servidor
                    enviarMensagem(socketChannel, mensagem);  
                    // Fecha a conexão
                    socketChannel.close();  
                    System.out.println("Conexão encerrada.");
                    break;
                }
            }
        }
        
    }
    
    /*
     * Método que armazena todas as interações para realizar a compra do cliente
     */
    private static void iniciarCompra(SocketChannel socketChannel) throws IOException {
        // Solicitar ao servidor as cidades disponíveis
        enviarMensagem(socketChannel, "iniciar_compra,");
        String respostaCidades = receberResposta(socketChannel);
    
        // Mapeamento das cidades recebidas seguindo o padrão (número -> nome da cidade)
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
    
        Scanner scanner = new Scanner(System.in);
        String origemInput;
        String destinoInput;
        boolean origemValida = false;
        boolean destinoValido = false;
    
        try {
            // Loop para validar a cidade de origem
            do {
                System.out.print("Digite o número correspondente à cidade de origem (entre 1 e 10): ");
                origemInput = scanner.nextLine();
    
                // Verifica se o número da origem está entre 1 e 10
                if (origemInput.matches("[1-9]|10")) {
                    origemValida = true;
                } else {
                    System.out.println("Número inválido. A cidade de origem deve ser um número entre 1 e 10.");
                }
            } while (!origemValida);
    
            String cidadeOrigem = cidadesMap.get(Integer.parseInt(origemInput));
    
            // Loop para validar a cidade de destino
            do {
                System.out.print("Digite o número correspondente à cidade de destino (entre 1 e 10): ");
                destinoInput = scanner.nextLine();
    
                // Verifica se o número do destino está entre 1 e 10 e se é diferente da origem
                if (destinoInput.matches("[1-9]|10")) {
                    if (!destinoInput.equals(origemInput)) {
                        destinoValido = true;
                    } else {
                        System.out.println("Impossível comprar passagem para o mesmo local de origem!");
                    }
                } else {
                    System.out.println("Número inválido. A cidade de destino deve ser um número entre 1 e 10.");
                }
            } while (!destinoValido);
    
            String cidadeDestino = cidadesMap.get(Integer.parseInt(destinoInput));
    
            // Enviar as cidades escolhidas ao servidor para listar rotas
            String mensagemEscolherCidades = "escolher_cidades," + cidadeOrigem + "," + cidadeDestino;
            enviarMensagem(socketChannel, mensagemEscolherCidades);
    
            String respostaRotas = receberResposta(socketChannel);
    
            String rotaInput;
            boolean rotaValida = false;
    
            // Loop para garantir que o usuário digite apenas números ao escolher a rota
            do {
                System.out.print("Digite o número correspondente à rota escolhida: ");
                rotaInput = scanner.nextLine();
    
                // Verifica se o input contém apenas dígitos numéricos
                if (rotaInput.matches("\\d+")) {
                    rotaValida = true;
                } else {
                    System.out.println("Entrada inválida. Por favor, digite apenas números.");
                }
            } while (!rotaValida);
    
            String mensagemEscolherRota = "escolher_rota," + rotaInput + "," + cidadeDestino;
            enviarMensagem(socketChannel, mensagemEscolherRota);
    
            // Receber confirmação da compra
            String respostaCompra = receberResposta(socketChannel);
    
        } catch (Exception e) {
            erro_comunicacao();
        }
    }
    
    /*
     * Método que realiza o envio da mensagem ao servidor
     */
    private static void enviarMensagem(SocketChannel socketChannel, String mensagem) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(mensagem.getBytes());
        socketChannel.write(buffer);
        buffer.clear();
    }

    /*
     * Método responsável por esperar e recever a resposta do servidor
     */
    private static String receberResposta(SocketChannel socketChannel) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(8192);
    StringBuilder resposta = new StringBuilder();
    int bytesRead;
    long startTime = System.currentTimeMillis();
     // Timeout de 5 segundos
    long timeoutMillis = 5000;

    while (true) {
        bytesRead = socketChannel.read(buffer);

        if (bytesRead > 0) {
            buffer.flip();
            System.out.print("\033[H\033[2J");
            System.out.flush();
            while (buffer.hasRemaining()) {
                resposta.append((char) buffer.get());
            }
            buffer.clear();
            break; // Sai do loop quando a resposta for recebida
        } else if (bytesRead == -1) {
            throw new IOException("Conexão fechada pelo servidor.");
        }

        // Verifica se o tempo de espera excedeu o timeout
        if (System.currentTimeMillis() - startTime > timeoutMillis) {
            throw new IOException("Tempo de resposta do servidor esgotado.");
        }

        // Pequeno delay para evitar CPU overloading
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    System.out.println("Resposta do servidor: " + resposta);
    return resposta.toString();
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
