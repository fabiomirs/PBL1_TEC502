import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class AtendeCliente extends Thread {
    // Mapa que armazena os trajetos e a quantidade de passagens disponíveis
    private HashMap<String, Integer> Lista_de_trajetos = new HashMap<>();
    // Objeto estático que representa uma passagem
    public static Passagem passagem = new Passagem(10, "Salvador - Recife");
    private Socket clientSocket;

    // Construtor que recebe o socket do cliente
    public AtendeCliente(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    // Método que atualiza a lista de trajetos e envia de volta ao cliente
    public Pacote_pro_servidor Atualizar_trajetos(Pacote_pro_servidor pacote_pro_servidor) {
        // Adiciona o trajeto atual na lista
        Lista_de_trajetos.put(passagem.getTrajeto(), passagem.getQuantidade());
        // Atualiza o pacote com a lista de trajetos
        pacote_pro_servidor.setLista_de_trajetos(Lista_de_trajetos);
        return pacote_pro_servidor;
    }

    // Método que processa a compra de passagem
    public Pacote_pro_servidor vende_passagem(Pacote_pro_servidor pacote_pro_servidor) {
        // Atualiza o pacote com a passagem disponível
        pacote_pro_servidor.setPassagem(passagem);
        int compre_feita = passagem.getQuantidade();
        
        // Verifica se ainda há passagens disponíveis
        if (compre_feita > 0) {
            // Adiciona o passageiro à lista da passagem
            passagem.adicionar_passageiro(pacote_pro_servidor.getPessoa());
            // Atualiza o pacote com uma mensagem de sucesso
            pacote_pro_servidor.setMensagem("Passagem comprada com sucesso!");
        } else {
            // Atualiza o pacote com uma mensagem de erro
            pacote_pro_servidor.setMensagem("Impossível comprar passagem");
        }

        return pacote_pro_servidor;
    }

    @Override
    public void run() {
        // Exibe uma mensagem quando um cliente se conecta
        System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            // Cria o stream de saída para enviar objetos ao cliente
            ObjectOutputStream envia_cliente = new ObjectOutputStream(clientSocket.getOutputStream());

            while (true) {
                // Lê o pacote enviado pelo cliente
                Pacote_pro_servidor pacote_pro_servidor = (Pacote_pro_servidor) in.readObject();
                Pacote_pro_servidor pacote_pro_cliente;

                // Verifica o ID do pacote para determinar a ação
                if (pacote_pro_servidor.getId() == 1) {
                    // ID 1: Atualizar trajetos disponíveis
                    pacote_pro_cliente = Atualizar_trajetos(pacote_pro_servidor);
                } else {
                    // Outro ID: Vender passagem
                    pacote_pro_cliente = vende_passagem(pacote_pro_servidor);
                }

                // Envia o pacote atualizado de volta ao cliente
                envia_cliente.writeObject(pacote_pro_cliente);
            }

        } catch (ClassNotFoundException | IOException e) {
            // Captura e imprime erros de classe não encontrada ou de entrada/saída
            e.printStackTrace();
        }
    }
}
