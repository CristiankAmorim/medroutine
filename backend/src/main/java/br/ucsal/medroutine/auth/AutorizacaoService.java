package br.ucsal.medroutine.auth;

import br.ucsal.medroutine.model.DoseProgramada;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.repository.UsuarioRepository;
import br.ucsal.medroutine.repository.VinculoRepository;
import br.ucsal.medroutine.service.ApiException;
import org.springframework.stereotype.Service;

@Service
public class AutorizacaoService {
    private final UsuarioRepository usuarioRepository;
    private final VinculoRepository vinculoRepository;

    public AutorizacaoService(UsuarioRepository usuarioRepository, VinculoRepository vinculoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.vinculoRepository = vinculoRepository;
    }

    public Usuario carregar(UsuarioAutenticado autenticado) {
        return usuarioRepository.findById(autenticado.id())
                .orElseThrow(() -> ApiException.naoAutorizado("Sessão inválida. Entre novamente."));
    }

    public Usuario exigirIdosoVinculado(UsuarioAutenticado autenticado, Long idosoId) {
        if (!autenticado.isCuidador()
                || !vinculoRepository.existsByCuidadorIdAndIdosoIdAndAtivoTrue(autenticado.id(), idosoId)) {
            throw ApiException.acessoNegado();
        }
        return usuarioRepository.findById(idosoId)
                .orElseThrow(() -> ApiException.naoEncontrado("Idoso não encontrado."));
    }

    public void exigirAcessoAoIdoso(UsuarioAutenticado autenticado, Long idosoId) {
        if (autenticado.isIdoso()) {
            if (!autenticado.id().equals(idosoId)) {
                throw ApiException.acessoNegado();
            }
            return;
        }
        if (!vinculoRepository.existsByCuidadorIdAndIdosoIdAndAtivoTrue(autenticado.id(), idosoId)) {
            throw ApiException.acessoNegado();
        }
    }

    public void exigirAcessoADose(UsuarioAutenticado autenticado, DoseProgramada dose) {
        exigirAcessoAoIdoso(autenticado, dose.getIdoso().getId());
    }
}
