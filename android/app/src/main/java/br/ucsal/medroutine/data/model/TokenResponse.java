package br.ucsal.medroutine.data.model;

public class TokenResponse {
    public String token;
    public Usuario usuario;

    public TokenResponse() { }

    public TokenResponse(String token, Usuario usuario) {
        this.token = token;
        this.usuario = usuario;
    }
}
