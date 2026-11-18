package br.ucsal.medroutine.data.model;

public class ErroApi {
    public String mensagem;
    public String campo;
    public transient int status;
    public transient boolean semConexao;

    public ErroApi() { }

    public ErroApi(String mensagem, String campo) {
        this.mensagem = mensagem;
        this.campo = campo;
    }

    public static ErroApi conexao() {
        ErroApi e = new ErroApi("Sem conexão com o servidor. Verifique a internet e tente novamente.", null);
        e.semConexao = true;
        return e;
    }
}
