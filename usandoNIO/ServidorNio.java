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
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;



public class ServidorNio {
    private static ConcurrentHashMap<SocketChannel, String> clientesConectados = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, SocketChannel> clientesConectadosPorCPF = new ConcurrentHashMap<>();
   // private static ConcurrentHashMap<String, Integer> trechosDisponiveis = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, Map<String,Integer>> trechos; // pros grafos


    private static HashMap<String, String> origem_dos_clientes;
    


    public static void main(String[] args) throws IOException {
        ServidorNio grafo = new ServidorNio();
        origem_dos_clientes = new HashMap<>();

        // Adicionar algumas arestas direcionadas (origem → destino)
        grafo.adicionarCidade("Sao Paulo", "Rio de Janeiro", 5);
        grafo.adicionarCidade("Sao Paulo", "Brasilia", 10);
        grafo.adicionarCidade("Rio de Janeiro", "Sao Paulo", 5);
        grafo.adicionarCidade("Rio de Janeiro", "Brasilia", 8);
        grafo.adicionarCidade("Brasilia", "Sao Paulo", 10);
        grafo.adicionarCidade("Brasilia", "Rio de Janeiro", 8);
        grafo.adicionarCidade("Brasilia", "Salvador", 9);
        grafo.adicionarCidade("Salvador", "Brasilia", 9);
        grafo.adicionarCidade("Salvador", "Recife", 4);
        grafo.adicionarCidade("Fortaleza", "Recife", 5);
        grafo.adicionarCidade("Fortaleza", "Brasilia", 12);
        grafo.adicionarCidade("Belo Horizonte", "Sao Paulo", 6);
        grafo.adicionarCidade("Belo Horizonte", "Rio de Janeiro", 7);
        grafo.adicionarCidade("Manaus", "Brasilia", 15);
        grafo.adicionarCidade("Curitiba", "Sao Paulo", 4);
        grafo.adicionarCidade("Curitiba", "Porto Alegre", 6);
        grafo.adicionarCidade("Porto Alegre", "Curitiba", 6);
        grafo.adicionarCidade("Recife", "Salvador", 4);
        grafo.adicionarCidade("Recife", "Fortaleza", 5);




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
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
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
    
            if (codigo.equals("1")) {
                if (trechos.containsKey(cidade)) {
                    origem_dos_clientes.put(cpf, cidade);
                    String resposta = "De acordo com seu local de Origem: " + origem_dos_clientes.get(cpf) + "\n" +
                                      "Temos os seguintes destinos {destino=passagens} ---> " + trechos.get(cidade).toString();
                    clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
                } else {
                    String resposta = "Desculpa, mas com base nessa origem, não temos destinos disponíveis";
                    clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
                }
            } else if (codigo.equals("2")) {
                if (trechos.get(origem_dos_clientes.get(cpf)).containsKey(cidade)) {
                    processarCompra(clientChannel, cidade, cpf);
                } else {
                    String resposta = "Desculpa, mas não temos trajetos para esse destino";
                    clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
                }
            }
        } else if (acao.equals("listar")) {
            listarTrechos(clientChannel, cpf);
        } else if (acao.equals("listar_rotas")) {
            String origem = parts[1];
            String destino = parts[2];
            List<List<String>> rotas = encontrarRotasBFS(origem, destino);
            StringBuilder resposta = new StringBuilder("Rotas possíveis de " + origem + " para " + destino + ":\n");
            if (rotas.isEmpty()) {
                resposta.append("Nenhuma rota encontrada.");
            } else {
                for (List<String> rota : rotas) {
                    resposta.append(String.join(" -> ", rota)).append("\n");
                }
            }
            clientChannel.write(ByteBuffer.wrap(resposta.toString().getBytes()));
        } else if (acao.equals("sair")) {
            clientesConectados.remove(clientChannel);
            clientesConectadosPorCPF.remove(cpf);
            System.out.println("Cliente com CPF " + cpf + " saiu.");
            clientChannel.write(ByteBuffer.wrap("Conexão encerrada.".getBytes()));
            clientChannel.close();
        }
    }
    




    private static synchronized void processarCompra(SocketChannel clientChannel, String cidade_destino, String cpf) throws IOException {
        
        int qnt_passagens  = trechos.get(origem_dos_clientes.get(cpf)).get(cidade_destino);
        if(qnt_passagens <= 0){
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
        //System.out.println(resultado);
        return resultado;
    }
    
    public static List<List<String>> encontrarRotasBFS(String origem, String destino) {
        List<List<String>> rotas = new ArrayList<>();
        Queue<List<String>> fila = new LinkedList<>();
        boolean[] visitado = new boolean[trechos.size()];
    
        // Inicializa a fila com o caminho inicial contendo apenas a origem
        List<String> caminhoInicial = new ArrayList<>();
        caminhoInicial.add(origem);
        fila.add(caminhoInicial);
    
        while (!fila.isEmpty()) {
            List<String> caminhoAtual = fila.poll();
            String ultimoNo = caminhoAtual.get(caminhoAtual.size() - 1);
    
            if (ultimoNo.equals(destino)) {
                rotas.add(new ArrayList<>(caminhoAtual));
            } else {
                int indexAtual = obterIndiceNo(ultimoNo);
                visitado[indexAtual] = true;
    
                Map<String, Integer> vizinhos = trechos.get(ultimoNo);
                if (vizinhos != null) {
                    for (String vizinho : vizinhos.keySet()) {
                        int indexVizinho = obterIndiceNo(vizinho);
                        if (!visitado[indexVizinho]) {
                            List<String> novoCaminho = new ArrayList<>(caminhoAtual);
                            novoCaminho.add(vizinho);
                            fila.add(novoCaminho);
                        }
                    }
                }
            }
        }
    
        return rotas;
    }
    
    private static int obterIndiceNo(String no) {
        int index = 0;
        for (String key : trechos.keySet()) {
            if (key.equals(no)) {
                return index;
            }
            index++;
        }
        return -1;
    }
    
}
