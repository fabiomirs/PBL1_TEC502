package usandoNIO;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;



public class ServidorNio {
    private static ConcurrentHashMap<SocketChannel, String> clientesConectados = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, SocketChannel> clientesConectadosPorCPF = new ConcurrentHashMap<>();
   // private static ConcurrentHashMap<String, Integer> trechosDisponiveis = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, Map<String,Long>> trechos; // pros grafos


    private static HashMap<String, String> origem_dos_clientes;
    


    public static void main(String[] args) throws IOException {
        ServidorNio grafo = new ServidorNio();
        origem_dos_clientes = new HashMap<>();

        // Caminho e nome do arquivo JSON
        String caminhoPasta = "dados";
        String nomeArquivo = "cidades.json";
        File arquivoJSON = new File(caminhoPasta, nomeArquivo);
        
        ler_cidades(arquivoJSON);

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
        System.out.println("clientes conectados sem cpf: "+clientesConectados);
        System.out.println("clientes conectados com cpf: "+clientesConectadosPorCPF);
        
        String[] parts = request.split(",");
        String acao = parts[0];
    
        if (acao.equals("iniciar_compra")) {
            //Mostra as 10 cidades disponíveis
            StringBuilder cidadesDisponiveis = new StringBuilder("Cidades disponíveis:\n");
            int count = 1;
            for (String cidade : trechos.keySet()) {
                cidadesDisponiveis.append(count).append(". ").append(cidade).append("\n");
                count++;
                if (count > 10) break; // Limitar a 10 cidades
            }
            clientChannel.write(ByteBuffer.wrap(cidadesDisponiveis.toString().getBytes()));
    
        } else if (acao.equals("escolher_cidades")) {
            //Recebe a origem e o destino escolhidos
            String cidadeOrigem = parts[1];
            String cidadeDestino = parts[2];
    
            if (trechos.containsKey(cidadeOrigem)) {
                origem_dos_clientes.put(cpf, cidadeOrigem);
    
                // Lista rotas possíveis
                List<List<String>> rotas = encontrarRotasBFS(cidadeOrigem, cidadeDestino);
                StringBuilder resposta = new StringBuilder("Rotas possíveis de " + cidadeOrigem + " para " + cidadeDestino + ":\n");
    
                if (rotas.isEmpty()) {
    resposta.append("Nenhuma rota encontrada.");
        } else {
            int rotaNumero = 1;
            for (List<String> rota : rotas) {
                resposta.append("Rota ").append(rotaNumero).append(":\n");

                Long passagensTotalDisponiveis = (long) Integer.MAX_VALUE; // valor inicial para as passagens 

                StringBuilder rotaDetalhes = new StringBuilder();
                for (int i = 0; i < rota.size() - 1; i++) {
                    String trechoOrigem = rota.get(i);
                    String trechoDestino = rota.get(i + 1);

                    // Verifica se o trecho existe antes de acessar os detalhes
                    if (trechos.containsKey(trechoOrigem) && trechos.get(trechoOrigem).containsKey(trechoDestino)) {


                        Long passagensDisponiveis = trechos.get(trechoOrigem).get(trechoDestino);


                        rotaDetalhes.append(trechoOrigem).append(" -> ").append(trechoDestino)
                                    .append(" (Passagens disponíveis: ").append(passagensDisponiveis).append(")");

                        // Atualiza a quantidade total de passagens disponíveis para a rota
                        if (passagensDisponiveis < passagensTotalDisponiveis) {
                            passagensTotalDisponiveis = passagensDisponiveis;
                        }

                        if (i < rota.size() - 2) {
                            rotaDetalhes.append(", ");
                        }
                    } else {
                        rotaDetalhes.append(trechoOrigem).append(" -> ").append(trechoDestino)
                                    .append(" (Informação de passagens não disponível)");
                    }
                }

                resposta.append(rotaDetalhes.toString()).append("\n");
                resposta.append("Total de passagens disponíveis para a rota: ").append(passagensTotalDisponiveis).append("\n\n");

                rotaNumero++;
            }
        }
                clientChannel.write(ByteBuffer.wrap(resposta.toString().getBytes()));
            } else {
                clientChannel.write(ByteBuffer.wrap("Cidade de origem inválida.".getBytes()));
            }
    
        } else if (acao.equals("escolher_rota")) {
            // Recebe a rota escolhida e processar a compra
            int rotaEscolhida = Integer.parseInt(parts[1]) - 1; // A rota será recebida a partir da escolha do cliente
            String cidadeOrigem = origem_dos_clientes.get(cpf);
            String cidadeDestino = parts[2];
            
            List<List<String>> rotas = encontrarRotasBFS(cidadeOrigem, cidadeDestino);
            if (rotaEscolhida >= 0 && rotaEscolhida < rotas.size()) {
                List<String> rota = rotas.get(rotaEscolhida);
    
                // Calcula as passagens disponíveis para cada trecho da rota
                Long passagensTotalDisponiveis = (long) Integer.MAX_VALUE;
                boolean sucesso = true;
                for (int i = 0; i < rota.size() - 1; i++) {
                    String trechoOrigem = rota.get(i);
                    String trechoDestino = rota.get(i + 1);
                    Long passagensDisponiveis = trechos.get(trechoOrigem).get(trechoDestino);
                    
                    // Atualiza a quantidade total de passagens disponíveis para a rota
                    if (passagensDisponiveis < passagensTotalDisponiveis) {
                        passagensTotalDisponiveis = passagensDisponiveis;
                    }
                    
                    if (passagensDisponiveis <= 0) {
                        sucesso = false;
                        break;
                    }
                }
    
                if (sucesso) {
                    // Processar a compra para todos os trechos da rota escolhida
                    for (int i = 0; i < rota.size() - 1; i++) {
                        String trechoOrigem = rota.get(i);
                        String trechoDestino = rota.get(i + 1);
                        Long passagensDisponiveis = trechos.get(trechoOrigem).get(trechoDestino);
                        trechos.get(trechoOrigem).put(trechoDestino, passagensDisponiveis - 1);
                    }
    
                    String resposta = "Compra efetuada com sucesso para a rota: " + String.join(" -> ", rota);
                    clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
                } else {
                    String resposta = "Falha na compra. Não há passagens suficientes para um dos trechos da rota.";
                    clientChannel.write(ByteBuffer.wrap(resposta.getBytes()));
                }
            } else {
                clientChannel.write(ByteBuffer.wrap("Rota inválida.".getBytes()));
            }
        }
    }

    // Construtor para inicializar o grafo
    public ServidorNio() {
        trechos = new ConcurrentHashMap<>(); // Inicializa o mapa de adjacência
    }


    // Método para adicionar uma aresta direcionada de origem para destino
    public void adicionarCidade(String origem, String destino,Long passagens) {
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
    
                Map<String, Long> vizinhos = trechos.get(ultimoNo);
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

    
    public static void ler_cidades(File arquivoJSON){
            // Ler dados do arquivo JSON e converter de volta para HashMap
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(arquivoJSON)) {
                // Faz o parsing do arquivo e o converte em um objeto JSONObject
                JSONObject jsonLido = (JSONObject) parser.parse(reader);

                // Converter JSONObject para HashMap
                //Map<String, Object> hashMapLido = new HashMap<>(jsonLido);

                //System.out.println(hashMapLido);
                trechos.putAll(jsonLido);
                System.out.println(trechos);
                System.out.println("passagens de salvador para fortaleza: "+trechos.get("Brasilia").get("Rio de Janeiro"));


            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
    }
    
}