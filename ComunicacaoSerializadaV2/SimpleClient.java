
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class SimpleClient {
    public static void main(String[] args) throws ClassNotFoundException {
        // Define o endereço e a porta do servidor
        String serverAddress = "localhost"; // Endereço do servidor
        int port = 8080; // Porta do servidor

        // Tenta estabelecer conexão com o servidor usando try-with-resources
        try (Socket socket = new Socket(serverAddress, port)) {
            System.out.println("Conectado ao servidor " + serverAddress + " na porta " + port);
            boolean sair_loop = true; // Controle do loop principal

            // Cria fluxos de entrada e saída para comunicação com o servidor
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());

            // Loop principal para interação com o usuário
            while (sair_loop) {
                Integer id;
                System.out.println("BEM VINDO A VENDAS DE PASSAGENS ONLINE!");
                System.out.println("[1] Comprar passagem");
                System.out.println("[2] Ver trechos");
                System.out.println("[3] Sair");

                // Lê a opção escolhida pelo usuário
                String opcao = digitar("escolha: ");
                int n = Integer.parseInt(opcao);

                if (n == 1) {
                    id = 0;
                    // Cria um pacote para solicitar a compra de passagem
                    Pacote_pro_servidor pacote_pro_servidor = Comprar_passagem(socket, out, entrada, id);
                    // Envia o pacote ao servidor e recebe a resposta
                    Pacote_pro_servidor pacote_vindo_servidor = troca_de_mensagem(pacote_pro_servidor, entrada, out);
                    // Exibe a mensagem recebida do servidor
                    System.out.println(pacote_vindo_servidor.getMensagem());

                    // Aguarda entrada do usuário para continuar
                    digitar("Enter para voltar");
                    // Limpa o console
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                } else if (n == 2) {
                    id = 1;
                    // Cria um pacote para solicitar a lista de trechos
                    Pacote_pro_servidor pacote_pro_servidor = Comprar_passagem(socket, out, entrada, id);
                    // Envia o pacote ao servidor e recebe a resposta
                    Pacote_pro_servidor pacote_vindo_servidor = troca_de_mensagem(pacote_pro_servidor, entrada, out);
                    // Exibe a lista de trechos recebida do servidor
                    System.out.println(pacote_vindo_servidor.getLista_de_trajetos());

                    // Aguarda entrada do usuário para continuar
                    digitar("Enter para voltar");
                    // Limpa o console
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                } else if (n == 3) {
                    // Encerra o loop e fecha a conexão
                    sair_loop = false;
                } else {
                    System.out.println("Opção inválida. Tente novamente.");
                }
            }
        } catch (IOException e) {
            // Trata exceções relacionadas à entrada/saída
            e.printStackTrace();
        }
    }

    // Método para ler entrada do usuário com uma mensagem personalizada
    public static String digitar(String x) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite seu " + x);
        String frase = scanner.nextLine();
        return frase;
    }

    // Método para criar um objeto Pessoa baseado no ID da operação
    public static Pessoa criar(Integer id) {
        Pessoa cliente1;
        if (id == 1) {
            // Caso para visualizar trechos, não é necessário informar dados pessoais
            cliente1 = new Pessoa(null, "null", null);
        } else {
            // Coleta informações do usuário para compra de passagem
            String nome = digitar("nome: ");
            String qntPassagem = digitar("Quantidade de passagens: ");
            int qnt_passagens = Integer.parseInt(qntPassagem);
            cliente1 = new Pessoa(nome, "null", qnt_passagens);
        }
        return cliente1;
    }

    // Método para criar o pacote a ser enviado ao servidor com base na operação desejada
    public static Pacote_pro_servidor Comprar_passagem(Socket socket, ObjectOutputStream out, ObjectInputStream entrada, Integer id) throws IOException {
        // Cria um objeto Pessoa com as informações necessárias
        Pessoa pessoa = criar(id);
        // Cria um pacote contendo o ID da operação e os dados da pessoa
        Pacote_pro_servidor pacote_pro_servidor = new Pacote_pro_servidor(id, pessoa);
        return pacote_pro_servidor;
    }

    // Método para enviar o pacote ao servidor e receber a resposta correspondente
    public static Pacote_pro_servidor troca_de_mensagem(Pacote_pro_servidor pacote_pro_servidor, ObjectInputStream entrada, ObjectOutputStream out) {
        try {
            // Define uma mensagem inicial antes do envio
            pacote_pro_servidor.setMensagem("servidor vai receber ainda");
            // Envia o objeto pacote ao servidor
            out.writeObject(pacote_pro_servidor);
            out.flush();

            // Lê a resposta do servidor
            Pacote_pro_servidor pacote_de_volta = (Pacote_pro_servidor) entrada.readObject();
            return pacote_de_volta;
        } catch (Exception e) {
            // Trata possíveis exceções durante a comunicação
            System.out.println("Você atingiu o limite de compra ou ocorreu um erro na comunicação.");
            e.printStackTrace();
        }
        return null;
    }
}
