import java.io.Serializable;
import java.util.HashMap;

public class Pacote_pro_servidor implements Serializable {
    private Integer id;
    private Pessoa pessoa;
    private Passagem passagem; 
    private String mensagem;
    private HashMap<String, Integer> Lista_de_trajetos = new HashMap<>();

    Pacote_pro_servidor(Integer id,Pessoa pessoa){
        this.id = id;
        if(id == 0){
            this.pessoa = pessoa;
        }
        

    }
    //pacote para trajetos
    /*------------------------------------------------------------------------------- */
    public HashMap getLista_de_trajetos(){
        return Lista_de_trajetos;   
    }

    public void setLista_de_trajetos(HashMap Lista_de_trajetos_do_servidor){
        Lista_de_trajetos.putAll(Lista_de_trajetos_do_servidor);
    }

    /*------------------------------------------------------------------------------- */

    public Pessoa getPessoa(){
        return pessoa;
    }

    public Passagem getPassagem(){
        return passagem;
    }

    public void setPassagem(Passagem passagem){
        this.passagem = passagem;
    }

    public void setMensagem(String mensagem){
        this.mensagem = mensagem;
    }
    
    public String getMensagem(){
        return this.mensagem;
    }

    public Integer getId(){
        return id;
    }


    
}
