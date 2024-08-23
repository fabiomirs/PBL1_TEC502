import java.io.IOException;
import java.io.PrintStream;  // Importa a classe para enviar dados ao servidor
import java.net.Socket;  // Importa a classe para criar a conexão com o servidor
import java.util.Scanner;  // Importa a classe para ler entradas do teclado

public class cliente {  // Nome da classe, seguindo a convenção de nomeação com a inicial maiúscula
    public static void main(String[] args) throws IOException {
        // Cria uma conexão com o servidor na porta 4000. "localhost" refere-se à máquina local
        Socket socket = new Socket("localhost", 4000);
        System.out.println("Conectado ao servidor!");
        
        // Cria um Scanner para ler as entradas do teclado do usuário
        Scanner scanner = new Scanner(System.in);

        // Inicia uma nova thread para lidar com a comunicação com o servidor
        ClienteThread clienteThread = new ClienteThread(socket);
        clienteThread.start();
        
        // Variável usada para sair do loop
        String sair = "";

        // Loop que continua a execução enquanto a variável 'sair' não for igual a "sair"
        while(!sair.equals("sair")){
            // Cria um PrintStream para enviar dados ao servidor
            PrintStream saida = new PrintStream(socket.getOutputStream());
            
            // Lê a entrada do teclado do usuário
            String teclado = scanner.nextLine();
            
            // Envia a mensagem digitada pelo usuário para o servidor
            saida.println(teclado);
            
            // Adiciona uma condição para sair do loop se o usuário digitar "sair"
            if (teclado.equals("sair")) {
                sair = "sair";  // Atualiza a variável 'sair' para terminar o loop
            }
        }
        
        // Fecha a conexão com o servidor e outros recursos
        socket.close();
        scanner.close();
    }
}
