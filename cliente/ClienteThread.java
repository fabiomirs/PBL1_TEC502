package cliente;// Importa classes necessárias para leitura de dados e manipulação de rede
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

// Define a classe cliente.ClienteThread que estende a classe Thread, permitindo execução em paralelo
public class ClienteThread extends Thread {
    // Declaração de um objeto Socket para conectar-se ao servidor
    private Socket socket;

    // Construtor da classe que inicializa o objeto Socket
    public ClienteThread(Socket socket) {
        this.socket = socket; // Atribui o socket recebido ao atributo da classe
    }

    // Sobrescreve o método run() da classe Thread para definir o comportamento da thread
    @Override
    public void run() {
        try {
            // Cria um InputStreamReader para ler bytes do socket e convertê-los em caracteres
            InputStreamReader inputReader = new InputStreamReader(socket.getInputStream());
       
            // Cria um BufferedReader para ler texto a partir do InputStreamReader
            BufferedReader reader = new BufferedReader(inputReader);

            String x; // Variável para armazenar as linhas de texto recebidas
            
            // Loop para ler linhas do BufferedReader até que não haja mais dados (null)
            while ((x = reader.readLine()) != null) {
                // Imprime a mensagem recebida do servidor no console
                System.out.println("servidor.Servidor mandou uma mensagem: " + x);
            }
            
        } catch (Exception e) {
            // Captura e imprime qualquer exceção que ocorra durante a execução
            e.printStackTrace();
        }
    }
}
