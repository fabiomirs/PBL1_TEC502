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
   // private static ConcurrentHashMap<String, Integer> trechosDisponiveis = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, Map<String,Integer>> trechos; // pros grafos

    private static String origem_cliente;

    private static HashMap<String, String> origem_dos_clientes;
    

    



    public static void main(String[] args) throws IOException {
        ServidorNio grafo = new ServidorNio();
        origem_dos_clientes = new HashMap<>();

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
            String cidade = parts[1];
            String codigo = parts[2];
            

            if(codigo.equals("1")){
                if(trechos.containsKey(cidade)){
                    //origem_cliente = cidade;
                    origem_dos_clientes.put(cpf, cidade);
                    String resposta = "De acordo com seu local de Origem: "+origem_dos_clientes.get(cpf)+"\n"+"Temos os seguintes destinos {destino=passagens} ---> "+trechos.get(cidade).toString();
                    clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
                }
                else{
                    String resposta = "Desculpa, mas com base nessa origem, não temos destinos disponiveis";
                    clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
                }

            }
            else if(codigo.equals("2")){
                System.out.println("analisando se sua cidade tem de acordo com a origem");
                if(trechos.get(origem_dos_clientes.get(cpf)).containsKey(cidade)){
                    //processo pra fazer a venda
                    processarCompra(clientChannel, cidade, cpf);
                }
                else{
                    String resposta = "Desculpa, mas não temos trajetos para esse destino";
                    clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
                }
            }

            //processarCompra(clientChannel, cidade, cpf);
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




    private static synchronized void processarCompra(SocketChannel clientChannel, String cidade_destino, String cpf) throws IOException {
        
        int qnt_passagens  = trechos.get(origem_dos_clientes.get(cpf)).get(cidade_destino);
        if(qnt_passagens < 0){
            String resposta = "Desculpe, não há mais passagens disponíveis para o trecho " + cidade_destino;
            clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
        }else{
            int novo_passagens = qnt_passagens-1;
            trechos.get(origem_dos_clientes.get(cpf)).put(cidade_destino, novo_passagens);
            String resposta = "Passagem comprada para a cidade " + cidade_destino + " pelo cliente de CPF " + cpf + ". Restantes: " + novo_passagens;
            clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
        }

    }




    private static void listarTrechos(SocketChannel clientChannel, String cpf) throws IOException {
        StringBuilder response = new StringBuilder("Trechos disponíveis para o cliente de CPF " + cpf + ":\n"+mostrarTrechos()); 

        ByteBuffer respostaBuffer = ByteBuffer.wrap(response.toString().getBytes()); 
                            
        // Enviar a resposta ao cliente
        clientChannel.write(respostaBuffer);
    }


    // Construtor para inicializar o grafo
    public ServidorNio() {
        trechos = new ConcurrentHashMap<>(); // Inicializa o mapa de adjacência
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

    // Método para imprimir a lista de adjacência do grafo
    public static String mostrarTrechos() {
        String resultado = trechos.toString();
        return resultado;
    }
}
