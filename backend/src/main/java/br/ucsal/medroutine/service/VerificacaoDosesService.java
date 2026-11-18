package br.ucsal.medroutine.service;

import br.ucsal.medroutine.domain.Relogio;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.model.DoseProgramada;
import br.ucsal.medroutine.model.TipoNotificacao;
import br.ucsal.medroutine.repository.DoseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class VerificacaoDosesService {
    private static final Logger log = LoggerFactory.getLogger(VerificacaoDosesService.class);

    private final DoseRepository doseRepository;
    private final NotificacaoService notificacaoService;
    private final TransactionTemplate transacao;
    private final Relogio relogio;

    public VerificacaoDosesService(DoseRepository doseRepository, NotificacaoService notificacaoService,
                                   TransactionTemplate transacao, Relogio relogio) {
        this.doseRepository = doseRepository;
        this.notificacaoService = notificacaoService;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void executar() {
        verificar(relogio.agora());
    }

    public int verificar(Instant agora) {
        int alertadas = 0;
        for (Long doseId : doseRepository.findIdsComToleranciaExcedida(agora)) {
            List<Long> ids = transacao.execute(status -> alertar(doseId, agora));
            if (ids != null && !ids.isEmpty()) {
                alertadas++;
                notificacaoService.enviar(ids);
            }
        }
        if (alertadas > 0) {
            log.info("Escalonamento: {} dose(s) não confirmada(s) geraram alerta.", alertadas);
        }
        return alertadas;
    }

    private List<Long> alertar(Long doseId, Instant agora) {
        DoseProgramada dose = doseRepository.findById(doseId).orElse(null);
        if (dose == null || !RegrasDose.deveAlertar(dose.getEstado(), dose.getDataHoraPrevista(),
                dose.getHorario().getToleranciaMinutos(), agora)) {
            return new ArrayList<>();
        }
        if (doseRepository.marcarNaoConfirmada(doseId) == 0) {
            return new ArrayList<>();
        }
        return notificacaoService.registrarParaCuidadores(dose, TipoNotificacao.ALERTA, null);
    }
}
