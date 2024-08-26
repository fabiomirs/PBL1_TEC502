package cliente;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) throws IOException {
        // Cria uma conexão com o servidor na porta 4000
        Socket socket = new Socket("servidor", 4000);
        System.out.println("Conectado ao servidor!");

        // Cria um Scanner para ler as entradas do teclado do usuário
        Scanner scanner = new Scanner(System.in);

        // Inicia uma nova thread para lidar com a comunicação com o servidor
        ClienteThread clienteThread = new ClienteThread(socket);
        clienteThread.start();

        String sair = "";

        while (!sair.equals("sair")) {
            PrintStream saida = new PrintStream(socket.getOutputStream());

            // Mostra as opções para o usuário
            System.out.println("Digite 'LISTAR TRECHOS' para ver os trechos disponíveis.");
            System.out.println("Digite 'RESERVAR <trecho> <quantidade>' para reservar um trecho com a quantidade desejada.");
            System.out.println("Digite 'sair' para encerrar.");

            // Lê a entrada do teclado do usuário
            String teclado = scanner.nextLine();

            // Envia a mensagem digitada pelo usuário para o servidor
            saida.println(teclado);

            // Adiciona uma condição para sair do loop se o usuário digitar "sair"
            if (teclado.equals("sair")) {
                sair = "sair";
            }
        }

        // Fecha a conexão com o servidor e outros recursos
        socket.close();
        scanner.close();
    }
}
