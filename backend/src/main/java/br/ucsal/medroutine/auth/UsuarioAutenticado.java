package br.ucsal.medroutine.auth;

import br.ucsal.medroutine.model.TipoUsuario;

public record UsuarioAutenticado(Long id, TipoUsuario tipo) {
    public boolean isCuidador() {
        return tipo == TipoUsuario.CUIDADOR;
    }

    public boolean isIdoso() {
        return tipo == TipoUsuario.IDOSO;
    }
}
