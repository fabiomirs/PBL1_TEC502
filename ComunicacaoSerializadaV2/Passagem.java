import java.io.Serializable;
import java.util.HashMap;

public class Passagem implements Serializable{
    private Integer quantidade;
    private HashMap<String, Integer> Lista_de_passageiros = new HashMap<>();
    private String trajeto;


    //construtor
    public Passagem(Integer quantidade,String trajeto){
        this.quantidade = quantidade;
        this.trajeto = trajeto;

    }

    public Integer getQuantidade(){
        return this.quantidade;
    }

    public Boolean adicionar_passageiro(Pessoa pessoa){
        if(this.quantidade <= 0){
            return false;

        }else{
            Lista_de_passageiros.put(pessoa.getNome(), pessoa.getQntPassagens());
            this.quantidade -= pessoa.getQntPassagens();
            return true;
        }
        
    }

    public String getTrajeto(){
        return this.trajeto;
    }


}
