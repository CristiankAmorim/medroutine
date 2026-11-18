package br.ucsal.medroutine.service;

import br.ucsal.medroutine.domain.Relogio;
import br.ucsal.medroutine.model.ConfirmacaoDose;
import br.ucsal.medroutine.model.DoseProgramada;
import br.ucsal.medroutine.model.Notificacao;
import br.ucsal.medroutine.model.TipoNotificacao;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.repository.ConfirmacaoRepository;
import br.ucsal.medroutine.repository.NotificacaoRepository;
import br.ucsal.medroutine.repository.UsuarioRepository;
import br.ucsal.medroutine.repository.VinculoRepository;
import br.ucsal.medroutine.web.dto.NotificacaoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class NotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    public static final int TENTATIVAS_MAXIMAS = 3;

    private final NotificacaoRepository notificacaoRepository;
    private final ConfirmacaoRepository confirmacaoRepository;
    private final VinculoRepository vinculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificationSender sender;
    private final Relogio relogio;

    public NotificacaoService(NotificacaoRepository notificacaoRepository, ConfirmacaoRepository confirmacaoRepository,
                              VinculoRepository vinculoRepository, UsuarioRepository usuarioRepository,
                              NotificationSender sender, Relogio relogio) {
        this.notificacaoRepository = notificacaoRepository;
        this.confirmacaoRepository = confirmacaoRepository;
        this.vinculoRepository = vinculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.sender = sender;
        this.relogio = relogio;
    }

    public List<Long> registrarParaCuidadores(DoseProgramada dose, TipoNotificacao tipo, Long excluirUsuarioId) {
        List<Long> ids = new ArrayList<>();
        for (Usuario cuidador : vinculoRepository.findCuidadoresAtivos(dose.getIdoso().getId())) {
            if (Objects.equals(cuidador.getId(), excluirUsuarioId)) {
                continue;
            }
            Notificacao n = notificacaoRepository.save(new Notificacao(dose, cuidador, tipo, relogio.agora()));
            ids.add(n.getId());
        }
        return ids;
    }

    public void enviar(List<Long> ids) {
        for (Long id : ids) {
            try {
                enviarUma(id);
            } catch (RuntimeException e) {
                log.error("Erro inesperado ao enviar a notificação {}", id, e);
            }
        }
    }

    private void enviarUma(Long id) {
        Notificacao n = notificacaoRepository.findCompleta(id).orElse(null);
        if (n == null) {
            return;
        }
        ConfirmacaoDose confirmacao = n.getTipo() == TipoNotificacao.CONFIRMACAO
                ? confirmacaoRepository.findByDoseIds(List.of(n.getDose().getId())).stream().findFirst().orElse(null)
                : null;
        Map<String, String> dados = MensagensNotificacao.montar(n, confirmacao);
        String token = n.getDestinatario().getFcmToken();

        if (token == null || token.isBlank()) {
            n.registrarTentativa(false, "Destinatário sem aparelho registrado para notificações.", relogio.agora());
            n.marcarFalha();
            notificacaoRepository.save(n);
            return;
        }
        for (int tentativa = 1; tentativa <= TENTATIVAS_MAXIMAS; tentativa++) {
            try {
                sender.enviar(token, dados);
                n.registrarTentativa(true, null, relogio.agora());
                break;
            } catch (NotificationSender.EnvioNotificacaoException e) {
                n.registrarTentativa(false, e.getMessage(), relogio.agora());
                log.warn("Falha na tentativa {} da notificação {}: {}", tentativa, id, e.getMessage());
                if (!e.isTemporaria() || tentativa == TENTATIVAS_MAXIMAS) {
                    n.marcarFalha();
                    break;
                }
                esperar(500L * tentativa);
            }
        }
        notificacaoRepository.save(n);
    }

    public void avisarAgendaAtualizada(Long idosoId) {
        usuarioRepository.findById(idosoId)
                .map(Usuario::getFcmToken)
                .filter(t -> !t.isBlank())
                .ifPresent(token -> {
                    try {
                        sender.enviar(token, Map.of("tipo", "AGENDA_ATUALIZADA"));
                    } catch (NotificationSender.EnvioNotificacaoException e) {
                        log.info("Não foi possível avisar o aparelho do idoso {}: {}", idosoId, e.getMessage());
                    }
                });
    }

    @Transactional(readOnly = true)
    public List<NotificacaoDTO> listarDoCuidador(Long cuidadorId) {
        return notificacaoRepository.findDoCuidador(cuidadorId, PageRequest.of(0, 100)).stream()
                .map(NotificacaoService::toDTO)
                .toList();
    }

    static NotificacaoDTO toDTO(Notificacao n) {
        DoseProgramada d = n.getDose();
        return new NotificacaoDTO(n.getId(), d.getId(), n.getTipo(), n.getEstadoEnvio(), n.getTentativas(),
                n.getCriadoEm(), n.getEnviadoEm(), n.getDestinatario().getNome(),
                d.getIdoso().getId(), d.getIdoso().getNome(), d.getMedicamento().getNome(),
                d.getMedicamento().getDosagem(), d.getDataHoraPrevista(), d.getHorario().getToleranciaMinutos(),
                d.getEstado());
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
