import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket; // Importa a classe para criar um servidor
import java.net.Socket; // Importa a classe para criar sockets (conexões)
import java.util.Scanner; // Importa a classe para leitura do teclado

public class servidor {
    public static void main(String[] args) throws IOException {
        // Cria um ServerSocket na porta 4000 para esperar conexões de clientes
        ServerSocket serverSocket = new ServerSocket(4000);
        
        // Espera até que um cliente se conecte ao servidor
        Socket socket = serverSocket.accept();
        System.out.println("cliente conectou\n");

        // Cria um InputStreamReader para ler os dados que vêm do cliente
        InputStreamReader inputReader = new InputStreamReader(socket.getInputStream());
        
        // Usa um BufferedReader para ler os dados do cliente linha por linha
        BufferedReader reader = new BufferedReader(inputReader);
        
        // Cria um PrintStream para enviar dados de volta para o cliente
        PrintStream saida = new PrintStream(socket.getOutputStream());
        
        // Variável para armazenar as mensagens recebidas do cliente
        String x;

        // Cria um Scanner para ler entradas do teclado no servidor
        Scanner scanner = new Scanner(System.in);

        // Loop que continua enquanto houver mensagens sendo enviadas pelo cliente
        while((x = reader.readLine()) != null) {
            // Exibe a mensagem recebida do cliente
            System.out.println("cliente mandou uma mensagem: " + x);
            
            // Lê a entrada do teclado no servidor
            String teclado = scanner.nextLine();
            
            // Envia a mensagem digitada pelo servidor de volta ao cliente
            saida.println(teclado);
        }
        
        // Fecha o ServerSocket após a comunicação terminar
        serverSocket.close();
    }
}
