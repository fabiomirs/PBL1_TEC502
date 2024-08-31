import java.io.Serializable;

public class Pessoa implements Serializable{
    private static final long serialVersionUID = 1L;
    private String nome;
    private String id;
    private Integer qnt_passagens;

    public Pessoa(String nome, String id,Integer qnt_passagens){
        this.id = id;
        this.nome = nome;
        this.qnt_passagens = qnt_passagens;
    }

    @Override
    public String toString(){
        return "Nome: "+ nome +", id: " + id; 
    }

    public String getNome(){
        return nome;
    }

    public Integer getQntPassagens(){
        return qnt_passagens;
    }




}
