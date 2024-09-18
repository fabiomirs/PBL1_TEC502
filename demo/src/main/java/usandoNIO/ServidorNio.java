package usandoNIO;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
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

    private static ConcurrentHashMap<String, Map<String,Long>> trechos; // pros grafos


    private static HashMap<String, String> origem_dos_clientes;
    private static HashMap<String,List> Registro_de_compra;
    


    public static void main(String[] args) throws IOException {
        trechos = new ConcurrentHashMap<>();
        
        origem_dos_clientes = new HashMap<>();
        Registro_de_compra = new HashMap<>();

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

                        //for para remover o cpf do cliente dos clientes conectados quando ocorre a interrupção
                        for(String clientes_conectados : clientesConectadosPorCPF.keySet()){
                            if(clientesConectadosPorCPF.get(clientes_conectados).isOpen() == false){
                                clientesConectadosPorCPF.remove(clientes_conectados);
                            }
                            
                        }
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
    
        if (acao.equals("iniciar_compra")) {
            // Mostra as 10 cidades disponíveis
            StringBuilder cidadesDisponiveis = new StringBuilder("Cidades disponíveis:\n");
            int count = 1;
            for (String cidade : trechos.keySet()) {
                cidadesDisponiveis.append(count).append(". ").append(cidade).append("\n");
                count++;
                if (count > 10) break; // Limitar a 10 cidades
            }
            clientChannel.write(ByteBuffer.wrap(cidadesDisponiveis.toString().getBytes()));
    
        } else if (acao.equals("escolher_cidades")) {
            // Recebe a origem e o destino escolhidos
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
    
                        // Calcula o preço da rota
                        int numeroCidades = rota.size(); // Inclui a cidade de origem e destino
                        double precoRota = 5000.0 / numeroCidades;
    
                        // Adiciona os detalhes da rota, o número de passagens disponíveis e o preço
                        resposta.append(rotaDetalhes.toString()).append("\n");
                        resposta.append("Total de passagens disponíveis para a rota: ").append(passagensTotalDisponiveis).append("\n");
                        resposta.append("Preço da rota: R$ ").append(String.format("%.2f", precoRota)).append("\n\n");
    
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
                    atualizar_arquivo_cidades();
                    String resposta = "Compra efetuada com sucesso para a rota: " + String.join(" -> ", rota);
    
                    organiza_registro(cpf, rota);
    
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

                trechos.putAll(jsonLido);
                


            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
    }

    public static void atualizar_arquivo_cidades(){
        // Caminho e nome do arquivo JSON
        String caminhoPasta = "dados";
        String nomeArquivo = "cidades.json";
        File arquivoJSON = new File(caminhoPasta, nomeArquivo);



        File pasta = new File(caminhoPasta);
        if (!pasta.exists()) {
            if (pasta.mkdirs()) {
                System.out.println("Pasta criada com sucesso.");
            } else {
                System.out.println("Falha ao criar a pasta.");
                return;
            }
        }

        // Converter HashMap para JSONObject e salvar em arquivo JSON
        JSONObject jsonObject = new JSONObject(trechos);

        try (FileWriter file = new FileWriter(arquivoJSON)) {
            file.write(jsonObject.toJSONString());
            file.flush();
            System.out.println("Trechos atualizados com sucesso: " + arquivoJSON.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para organizar e registrar informações sobre uma rota associada a um CPF
    public static void organiza_registro(String cpf, List<String> rota) {
        // Cria uma nova lista para armazenar as informações da rota e a data/hora atual
        List<String> informacoes = new ArrayList<>();

        // Obtém a data e hora atual no formato LocalDateTime
        LocalDateTime dataHoraAtual = LocalDateTime.now();
        // Converte a data e hora atual para uma string no formato ISO (yyyy-MM-dd'T'HH:mm:ss.SSS)
        String data = dataHoraAtual.toString();

        // Adiciona todas as localidades da rota à lista de informações
        for (String c : rota) {
            informacoes.add(c);
        }
        // Adiciona a data e hora atual à lista de informações
        informacoes.add(data);

        // Adiciona a lista de informações ao mapa Registro_de_compra, associando-a ao CPF fornecido
        Registro_de_compra.put(cpf, informacoes);

        // Imprime o conteúdo do mapa Registro_de_compra no console
        System.out.println(Registro_de_compra);

        // Chama o método arquivar_registro_cliente para arquivar o mapa Registro_de_compra
        arquivar_registro_cliente(Registro_de_compra);
    }


    public static void arquivar_registro_cliente(HashMap<String, List> Registro_de_compra) {
        // Caminho e nome do arquivo JSON
        String caminhoPasta = "dados";
        String nomeArquivo = "registro_de_compra.json";
        File arquivoJSON = new File(caminhoPasta, nomeArquivo);
    
        // Verifica se a pasta existe, caso contrário, cria
        File pasta = new File(caminhoPasta);
        if (!pasta.exists()) {
            if (pasta.mkdirs()) {
                System.out.println("Pasta criada com sucesso.");
            } else {
                System.out.println("Falha ao criar a pasta.");
                return;
            }
        }
    
        // Se o arquivo JSON não existir, cria um novo arquivo
        if (!arquivoJSON.exists()) {
            try {
                arquivoJSON.createNewFile();
                System.out.println("Arquivo de registro criado com sucesso.");
            } catch (IOException e) {
                System.out.println("Erro ao criar o arquivo JSON.");
                e.printStackTrace();
                return;
            }
        }
    
        Map<String, List> conteudo_lido = new HashMap<>();
    
        if (arquivoJSON.length() > 0) {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(arquivoJSON)) {
                // Faz o parsing do arquivo e o converte em um objeto JSONObject
                JSONObject jsonLido = (JSONObject) parser.parse(reader);
                // Converte o JSONObject para HashMap
                for (Object key : jsonLido.keySet()) {
                    String cpf = (String) key;
                    conteudo_lido.put(cpf, (List) jsonLido.get(cpf));
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
    
        for (String cpf_temporario : Registro_de_compra.keySet()) {
            conteudo_lido.put(cpf_temporario, Registro_de_compra.get(cpf_temporario));
        }
    
        // Converter HashMap para JSONObject e salvar em arquivo JSON
        JSONObject jsonObject = new JSONObject(conteudo_lido);
        try (FileWriter file = new FileWriter(arquivoJSON)) {
            file.write(jsonObject.toJSONString());
            file.flush();
            System.out.println("Trechos atualizados com sucesso: " + arquivoJSON.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
}
