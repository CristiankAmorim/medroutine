package br.ucsal.medroutine.service;

import br.ucsal.medroutine.auth.AutorizacaoService;
import br.ucsal.medroutine.auth.UsuarioAutenticado;
import br.ucsal.medroutine.domain.EstadoDose;
import br.ucsal.medroutine.domain.Relogio;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.model.ConfirmacaoDose;
import br.ucsal.medroutine.model.DoseProgramada;
import br.ucsal.medroutine.model.TipoNotificacao;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.repository.ConfirmacaoRepository;
import br.ucsal.medroutine.repository.DoseRepository;
import br.ucsal.medroutine.web.dto.ConfirmacaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Service
public class ConfirmacaoService {
    private final DoseRepository doseRepository;
    private final ConfirmacaoRepository confirmacaoRepository;
    private final NotificacaoService notificacaoService;
    private final DoseService doseService;
    private final AutorizacaoService autorizacao;
    private final TransactionTemplate transacao;
    private final Relogio relogio;

    public ConfirmacaoService(DoseRepository doseRepository, ConfirmacaoRepository confirmacaoRepository,
                              NotificacaoService notificacaoService, DoseService doseService,
                              AutorizacaoService autorizacao, TransactionTemplate transacao, Relogio relogio) {
        this.doseRepository = doseRepository;
        this.confirmacaoRepository = confirmacaoRepository;
        this.notificacaoService = notificacaoService;
        this.doseService = doseService;
        this.autorizacao = autorizacao;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    private record Resultado(ConfirmacaoResponse resposta, List<Long> notificacoes) { }

    public ConfirmacaoResponse confirmar(UsuarioAutenticado autenticado, Long doseId, Instant horarioAparelho) {
        Resultado r = transacao.execute(status -> {
            DoseProgramada dose = doseRepository.findByIdParaAtualizar(doseId)
                    .orElseThrow(() -> ApiException.naoEncontrado("Dose não encontrada."));
            autorizacao.exigirAcessoADose(autenticado, dose);

            if (confirmacaoRepository.findByDoseId(doseId).isPresent()) {
                return new Resultado(new ConfirmacaoResponse(doseService.toDTO(dose, false), true), List.of());
            }
            Instant agora = relogio.agora();
            Instant momento = RegrasDose.momentoDaConfirmacao(dose.getDataHoraPrevista(), agora, horarioAparelho);
            EstadoDose novoEstado = RegrasDose.estadoAposConfirmacao(dose.getEstado(), dose.getDataHoraPrevista(),
                    dose.getHorario().getToleranciaMinutos(), momento);
            Usuario usuario = autorizacao.carregar(autenticado);
            confirmacaoRepository.save(new ConfirmacaoDose(dose, usuario, agora, horarioAparelho));
            dose.setEstado(novoEstado);
            List<Long> ids = notificacaoService.registrarParaCuidadores(dose, TipoNotificacao.CONFIRMACAO,
                    usuario.isCuidador() ? usuario.getId() : null);
            confirmacaoRepository.flush();
            return new Resultado(new ConfirmacaoResponse(doseService.toDTO(dose, false), false), ids);
        });
        notificacaoService.enviar(r.notificacoes());
        return r.resposta();
    }
}
