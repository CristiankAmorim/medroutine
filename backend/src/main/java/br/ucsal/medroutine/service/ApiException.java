package br.ucsal.medroutine.service;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException naoEncontrado(String mensagem) {
        return new ApiException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static ApiException acessoNegado() {
        return new ApiException(HttpStatus.FORBIDDEN, "Você não tem acesso a estes dados.");
    }

    public static ApiException conflito(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, mensagem);
    }

    public static ApiException requisicaoInvalida(String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensagem);
    }

    public static ApiException naoAutorizado(String mensagem) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensagem);
    }
}
