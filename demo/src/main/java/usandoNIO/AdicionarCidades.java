package usandoNIO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

public class AdicionarCidades {

    private static ConcurrentHashMap<String, Map<String, Long>> trechos; // Mapa de adjacência

    // Construtor da classe
    public AdicionarCidades() {
        trechos = new ConcurrentHashMap<>(); // Inicializa o mapa de adjacência
    }

    // Método para adicionar cidades e passagens
    public static void adicionarCidade(String origem, String destino, Long passagens) {
        // Se a origem não existir no mapa, inicializa a lista de adjacência
        trechos.putIfAbsent(origem, new HashMap<>());
        // Adiciona o destino à lista de adjacência da origem
        trechos.get(origem).put(destino, passagens);

        // Também garantimos que o vértice de destino esteja no mapa, mesmo que sem adjacências
        trechos.putIfAbsent(destino, new HashMap<>());
    }

    // Método principal
    public static void main(String[] args) {
        // Instancia a classe AdicionarCidades
        AdicionarCidades adicionador = new AdicionarCidades();
        
        // Adicionar algumas arestas direcionadas (origem → destino)
        adicionador.adicionarCidade("Sao Paulo", "Rio de Janeiro", 10L);
        adicionador.adicionarCidade("Sao Paulo", "Brasilia", 10L);
        adicionador.adicionarCidade("Rio de Janeiro", "Sao Paulo", 10L);
        adicionador.adicionarCidade("Rio de Janeiro", "Brasilia", 10L);
        adicionador.adicionarCidade("Brasilia", "Sao Paulo", 10L);
        adicionador.adicionarCidade("Brasilia", "Rio de Janeiro", 10L);
        adicionador.adicionarCidade("Brasilia", "Salvador", 10L);
        adicionador.adicionarCidade("Salvador", "Brasilia", 10L);
        adicionador.adicionarCidade("Salvador", "Recife", 10L);
        adicionador.adicionarCidade("Fortaleza", "Recife", 10L);
        adicionador.adicionarCidade("Fortaleza", "Brasilia", 10L);
        adicionador.adicionarCidade("Fortaleza", "Belo Horizonte", 10L);
        adicionador.adicionarCidade("Belo Horizonte", "Sao Paulo", 10L);
        adicionador.adicionarCidade("Belo Horizonte", "Rio de Janeiro", 10L);
        adicionador.adicionarCidade("Manaus", "Brasilia", 10L);
        adicionador.adicionarCidade("Brasilia", "Manaus", 10L);
        adicionador.adicionarCidade("Sao Paulo", "Curitiba", 10L);
        adicionador.adicionarCidade("Curitiba", "Sao Paulo", 10L);
        adicionador.adicionarCidade("Curitiba", "Porto Alegre", 10L);
        adicionador.adicionarCidade("Porto Alegre", "Curitiba", 10L);
        adicionador.adicionarCidade("Recife", "Salvador", 10L);
        adicionador.adicionarCidade("Recife", "Fortaleza", 10L);

        apagarRegistroDeCompra();
        salvar();
        
    }

    public static void salvar(){
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
            System.out.println("HashMap salvo no arquivo JSON com sucesso em: " + arquivoJSON.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     // Método para apagar o arquivo registro_de_compra.json
     public static void apagarRegistroDeCompra() {
        String caminhoPasta = "dados";
        String nomeArquivo = "registro_de_compra.json";
        File arquivoJSON = new File(caminhoPasta, nomeArquivo);

        if (arquivoJSON.exists()) {
            if (arquivoJSON.delete()) {
                System.out.println("Arquivo registro_de_compra.json apagado com sucesso.");
            } else {
                System.out.println("Falha ao apagar o arquivo registro_de_compra.json.");
            }
        } else {
            System.out.println("Arquivo registro_de_compra.json não encontrado.");
        }
    }

}



