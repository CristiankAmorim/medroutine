package br.ucsal.medroutine.domain;

public class ValidacaoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String campo;

    public ValidacaoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
