package br.ucsal.medroutine.service;

import br.ucsal.medroutine.auth.AutorizacaoService;
import br.ucsal.medroutine.auth.CodigoService;
import br.ucsal.medroutine.auth.UsuarioAutenticado;
import br.ucsal.medroutine.domain.Relogio;
import br.ucsal.medroutine.model.CodigoAcesso;
import br.ucsal.medroutine.model.TipoCodigo;
import br.ucsal.medroutine.model.TipoUsuario;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.model.VinculoCuidadorIdoso;
import br.ucsal.medroutine.repository.UsuarioRepository;
import br.ucsal.medroutine.repository.VinculoRepository;
import br.ucsal.medroutine.web.dto.CodigoDTO;
import br.ucsal.medroutine.web.dto.IdosoDTO;
import br.ucsal.medroutine.web.dto.IdosoRequest;
import br.ucsal.medroutine.web.dto.NovoIdosoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

@Service
public class IdosoService {
    private final UsuarioRepository usuarioRepository;
    private final VinculoRepository vinculoRepository;
    private final CodigoService codigoService;
    private final AutorizacaoService autorizacao;
    private final Relogio relogio;

    public IdosoService(UsuarioRepository usuarioRepository, VinculoRepository vinculoRepository,
                        CodigoService codigoService, AutorizacaoService autorizacao, Relogio relogio) {
        this.usuarioRepository = usuarioRepository;
        this.vinculoRepository = vinculoRepository;
        this.codigoService = codigoService;
        this.autorizacao = autorizacao;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<IdosoDTO> listar(UsuarioAutenticado cuidador) {
        return vinculoRepository.findIdososAtivos(cuidador.id()).stream().map(this::toDTO).toList();
    }

    @Transactional
    public NovoIdosoResponse criar(UsuarioAutenticado autenticado, IdosoRequest req) {
        Usuario cuidador = autorizacao.carregar(autenticado);
        Usuario idoso = new Usuario();
        idoso.setTipoUsuario(TipoUsuario.IDOSO);
        idoso.setNome(req.nome().trim());
        idoso.setFusoHorario(fusoValido(req.fusoHorario()));
        idoso.setAnuncioVoz(Boolean.TRUE.equals(req.anuncioVoz()));
        idoso.setConsentimentoEm(relogio.agora());
        usuarioRepository.save(idoso);
        vinculoRepository.save(new VinculoCuidadorIdoso(cuidador, idoso, relogio.agora()));
        CodigoService.CodigoGerado codigo = codigoService.gerar(TipoCodigo.ACESSO_IDOSO, idoso, cuidador);
        return new NovoIdosoResponse(toDTO(idoso), new CodigoDTO(codigo.codigo(), codigo.expiraEm()));
    }

    @Transactional
    public IdosoDTO atualizar(UsuarioAutenticado autenticado, Long idosoId, IdosoRequest req) {
        autorizacao.exigirIdosoVinculado(autenticado, idosoId);
        Usuario idoso = usuarioRepository.findById(idosoId).orElseThrow();
        idoso.setNome(req.nome().trim());
        if (req.fusoHorario() != null) {
            idoso.setFusoHorario(fusoValido(req.fusoHorario()));
        }
        if (req.anuncioVoz() != null) {
            idoso.setAnuncioVoz(req.anuncioVoz());
        }
        return toDTO(idoso);
    }

    @Transactional
    public CodigoDTO gerarCodigoAcesso(UsuarioAutenticado autenticado, Long idosoId) {
        Usuario idoso = autorizacao.exigirIdosoVinculado(autenticado, idosoId);
        CodigoService.CodigoGerado c = codigoService.gerar(TipoCodigo.ACESSO_IDOSO, idoso,
                autorizacao.carregar(autenticado));
        return new CodigoDTO(c.codigo(), c.expiraEm());
    }

    @Transactional
    public CodigoDTO gerarConvite(UsuarioAutenticado autenticado, Long idosoId) {
        Usuario idoso = autorizacao.exigirIdosoVinculado(autenticado, idosoId);
        CodigoService.CodigoGerado c = codigoService.gerar(TipoCodigo.CONVITE_CUIDADOR, idoso,
                autorizacao.carregar(autenticado));
        return new CodigoDTO(c.codigo(), c.expiraEm());
    }

    @Transactional
    public IdosoDTO aceitarConvite(UsuarioAutenticado autenticado, String codigo) {
        CodigoAcesso c = codigoService.consumir(codigo, TipoCodigo.CONVITE_CUIDADOR)
                .orElseThrow(() -> ApiException.requisicaoInvalida("Convite inválido, já utilizado ou expirado."));
        Usuario idoso = c.getUsuario();
        if (vinculoRepository.existsByCuidadorIdAndIdosoIdAndAtivoTrue(autenticado.id(), idoso.getId())) {
            throw ApiException.conflito("Você já acompanha " + idoso.getNome() + ".");
        }
        vinculoRepository.save(new VinculoCuidadorIdoso(autorizacao.carregar(autenticado), idoso, relogio.agora()));
        return toDTO(idoso);
    }

    @Transactional
    public void desvincular(UsuarioAutenticado autenticado, Long idosoId) {
        VinculoCuidadorIdoso v = vinculoRepository.findByCuidadorIdAndIdosoIdAndAtivoTrue(autenticado.id(), idosoId)
                .orElseThrow(ApiException::acessoNegado);
        v.desfazer(relogio.agora());
    }

    @Transactional
    public void excluir(UsuarioAutenticado autenticado, Long idosoId) {
        Usuario idoso = autorizacao.exigirIdosoVinculado(autenticado, idosoId);
        usuarioRepository.delete(idoso);
    }

    private IdosoDTO toDTO(Usuario idoso) {
        return new IdosoDTO(idoso.getId(), idoso.getNome(), idoso.getFusoHorario(), idoso.isAnuncioVoz(),
                vinculoRepository.countByIdosoIdAndAtivoTrue(idoso.getId()));
    }

    private static String fusoValido(String fuso) {
        if (fuso == null || fuso.isBlank()) {
            return "America/Bahia";
        }
        try {
            return ZoneId.of(fuso).getId();
        } catch (DateTimeException e) {
            throw ApiException.requisicaoInvalida("Fuso horário inválido.");
        }
    }
}
