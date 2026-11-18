package br.ucsal.medroutine.web.dto;

import br.ucsal.medroutine.model.TipoUsuario;
import br.ucsal.medroutine.model.Usuario;

public record UsuarioDTO(Long id, String nome, String email, TipoUsuario tipo, String fusoHorario, boolean anuncioVoz) {
    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNome(), u.getEmail(), u.getTipoUsuario(), u.getFusoHorario(),
                u.isAnuncioVoz());
    }
}
