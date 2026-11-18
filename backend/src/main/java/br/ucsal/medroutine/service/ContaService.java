package br.ucsal.medroutine.service;

import br.ucsal.medroutine.auth.UsuarioAutenticado;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.repository.UsuarioRepository;
import br.ucsal.medroutine.web.dto.UsuarioDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaService {
    private final UsuarioRepository usuarioRepository;

    public ContaService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioDTO dados(UsuarioAutenticado u) {
        return UsuarioDTO.de(carregar(u));
    }

    @Transactional
    public void registrarTokenAparelho(UsuarioAutenticado u, String token) {
        carregar(u).setFcmToken(token == null || token.isBlank() ? null : token);
    }

    @Transactional
    public void excluir(UsuarioAutenticado u) {
        usuarioRepository.delete(carregar(u));
    }

    private Usuario carregar(UsuarioAutenticado u) {
        return usuarioRepository.findById(u.id())
                .orElseThrow(() -> ApiException.naoAutorizado("Sessão inválida."));
    }
}
