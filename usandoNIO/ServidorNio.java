package usandoNIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorNio {
    private static ConcurrentHashMap<SocketChannel, String> clientesConectados = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, SocketChannel> clientesConectadosPorCPF = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Integer> trechosDisponiveis = new ConcurrentHashMap<>();

    private static Map<String, Map<String,Integer>> trechos; // pros grafos

    private static String origem_cliente;


    



    public static void main(String[] args) throws IOException {
        ServidorNio grafo = new ServidorNio();

        // Adicionar algumas arestas direcionadas (origem → destino)
        grafo.adicionarCidade("São Paulo", "Rio de Janeiro",2);
        grafo.adicionarCidade("São Paulo", "Belo Horizonte",2);
        grafo.adicionarCidade("Rio de Janeiro", "Belo Horizonte",2);
        grafo.adicionarCidade("Belo Horizonte", "São Paulo",2);
        grafo.adicionarCidade("Belo Horizonte", "Brasília",2);
        grafo.adicionarCidade("Brasília", "Brasília",2); // Loop em Brasília


        // Configuração do NIO
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(12345));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Servidor pronto para receber conexões...");

        while (true) {
            try {
                // Espera por eventos
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    try {
                        if (key.isAcceptable()) {
                            // Aceitar nova conexão
                            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                            SocketChannel clientChannel = serverChannel.accept();
                            clientChannel.configureBlocking(false);
                            clientChannel.register(selector, SelectionKey.OP_READ);
                            System.out.println("Novo cliente conectado: " + clientChannel.getRemoteAddress());
                        } else if (key.isReadable()) {
                            // Ler dados do cliente
                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(256);
                            int bytesRead = clientChannel.read(buffer);

                            if (bytesRead == -1) {
                                // Cliente desconectado
                                String cpf = clientesConectados.remove(clientChannel);
                                if (cpf != null) {
                                    clientesConectadosPorCPF.remove(cpf);
                                    System.out.println("Cliente com CPF " + cpf + " desconectado.");
                                }
                                clientChannel.close();
                            } else {
                                buffer.flip();
                                String request = new String(buffer.array()).trim();

                                // Se o cliente ainda não foi identificado, solicitar CPF
                                if (!clientesConectados.containsKey(clientChannel)) {
                                    validarERegistrarCPF(clientChannel, request);
                                } else {
                                    // Processar requisição após o CPF ser validado
                                    String cpf = clientesConectados.get(clientChannel);
                                    processarRequisicao(clientChannel, request, cpf);
                                }
                            }
                        }
                    } catch (IOException e) {
                        // Tratamento de exceção e continuar servidor
                        System.err.println("Erro na conexão: " + e.getMessage());
                        key.cancel(); // Remover a chave do cliente com erro
                        key.channel().close(); // Fechar o canal associado
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro geral: " + e.getMessage());
                // Continuar a execução do servidor após o erro
            }
        }
    }

    private static void validarERegistrarCPF(SocketChannel clientChannel, String request) throws IOException {
        if (clientesConectadosPorCPF.containsKey(request)) {
            // CPF já em uso, solicitar novo CPF
            clientChannel.write(ByteBuffer.wrap("CPF já em uso. Insira um CPF diferente:".getBytes()));
        } else {
            // Registrar CPF e cliente
            clientesConectados.put(clientChannel, request);
            clientesConectadosPorCPF.put(request, clientChannel);
            System.out.println("Cliente identificado com CPF: " + request);
            clientChannel.write(ByteBuffer.wrap(("CPF registrado: " + request).getBytes()));
        }
    }

    private static synchronized void processarRequisicao(SocketChannel clientChannel, String request, String cpf) throws IOException {
        String[] parts = request.split(",");
        String acao = parts[0];

        if (acao.equals("comprar")) {
            String trecho = parts[1];
            processarCompra(clientChannel, trecho, cpf);
        } else if (acao.equals("listar")) {
            listarTrechos(clientChannel, cpf);
        } else if (acao.equals("sair")) {
            // Desconectar cliente
            clientesConectados.remove(clientChannel);
            clientesConectadosPorCPF.remove(cpf);
            System.out.println("Cliente com CPF " + cpf + " saiu.");
            clientChannel.write(ByteBuffer.wrap("Conexão encerrada.".getBytes()));
            clientChannel.close();
        }
    }

    private static synchronized void processarCompra(SocketChannel clientChannel, String trecho, String cpf) throws IOException {
        if (trechosDisponiveis.containsKey(trecho)) {
            int passagensRestantes = trechosDisponiveis.get(trecho);

            if (passagensRestantes > 0) {
                trechosDisponiveis.put(trecho, passagensRestantes - 1);
                String resposta = "Passagem comprada para o trecho " + trecho + " pelo cliente de CPF " + cpf + ". Restantes: " + (passagensRestantes - 1);
                clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
            } else {
                String resposta = "Desculpe, não há mais passagens disponíveis para o trecho " + trecho;
                clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
            }
        } else {
            String resposta = "Trecho inválido!";
            clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
        }
    }



    // Construtor para inicializar o grafo
    public ServidorNio() {
        trechos = new HashMap<>(); // Inicializa o mapa de adjacência
    }


    // Método para adicionar uma aresta direcionada de origem para destino
    public void adicionarCidade(String origem, String destino,Integer passagens) {
        // Se a origem não existir no mapa, inicializa a lista de adjacência
        trechos.putIfAbsent(origem, new HashMap<>());
        // Adiciona o destino à lista de adjacência da origem
        trechos.get(origem).put(destino, passagens);
        // Também garantimos que o vértice de destino esteja no mapa, mesmo que sem adjacências
        trechos.putIfAbsent(destino, new HashMap<>());
    }

    
}
