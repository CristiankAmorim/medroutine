package br.ucsal.medroutine.service;

import br.ucsal.medroutine.auth.AutorizacaoService;
import br.ucsal.medroutine.auth.UsuarioAutenticado;
import br.ucsal.medroutine.domain.GeradorDoses;
import br.ucsal.medroutine.domain.Relogio;
import br.ucsal.medroutine.model.ConfirmacaoDose;
import br.ucsal.medroutine.model.DoseProgramada;
import br.ucsal.medroutine.model.HorarioMedicacao;
import br.ucsal.medroutine.model.Medicamento;
import br.ucsal.medroutine.model.Notificacao;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.repository.ConfirmacaoRepository;
import br.ucsal.medroutine.repository.DoseRepository;
import br.ucsal.medroutine.repository.HorarioRepository;
import br.ucsal.medroutine.repository.NotificacaoRepository;
import br.ucsal.medroutine.web.dto.ConfirmacaoDTO;
import br.ucsal.medroutine.web.dto.DoseDTO;
import br.ucsal.medroutine.web.dto.NotificacaoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DoseService {
    private static final Logger log = LoggerFactory.getLogger(DoseService.class);

    private final DoseRepository doseRepository;
    private final HorarioRepository horarioRepository;
    private final ConfirmacaoRepository confirmacaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final AutorizacaoService autorizacao;
    private final Relogio relogio;

    public DoseService(DoseRepository doseRepository, HorarioRepository horarioRepository,
                       ConfirmacaoRepository confirmacaoRepository, NotificacaoRepository notificacaoRepository,
                       AutorizacaoService autorizacao, Relogio relogio) {
        this.autorizacao = autorizacao;
        this.doseRepository = doseRepository;
        this.horarioRepository = horarioRepository;
        this.confirmacaoRepository = confirmacaoRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.relogio = relogio;
    }

    public int gerarDoses(HorarioMedicacao horario) {
        Medicamento m = horario.getMedicamento();
        List<Instant> previstos = GeradorDoses.gerarHorizonte(horario.toRegra(), m.getDataInicio(), m.getDataFim(),
                m.getIdoso().zona(), relogio);
        int criadas = 0;
        for (Instant previsto : previstos) {
            if (!doseRepository.existsByHorarioIdAndDataHoraPrevista(horario.getId(), previsto)) {
                doseRepository.save(new DoseProgramada(horario, previsto));
                criadas++;
            }
        }
        return criadas;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    @Transactional
    public void renovarHorizonte() {
        int total = 0;
        for (HorarioMedicacao h : horarioRepository.findAtivosComMedicamento()) {
            total += gerarDoses(h);
        }
        log.info("Horizonte de doses renovado: {} novas doses.", total);
    }

    @Transactional(readOnly = true)
    public List<DoseDTO> dosesDoDia(Usuario idoso, LocalDate data) {
        ZoneId zona = idoso.zona();
        Instant de = data.atStartOfDay(zona).toInstant();
        Instant ate = data.plusDays(1).atStartOfDay(zona).toInstant();
        return toDTOs(doseRepository.findPorIdosoEPeriodo(idoso.getId(), de, ate), false);
    }

    @Transactional(readOnly = true)
    public List<DoseDTO> historico(Usuario idoso, LocalDate inicio, LocalDate fim, Long medicamentoId) {
        if (fim.isBefore(inicio)) {
            throw ApiException.requisicaoInvalida("A data final não pode ser anterior à inicial.");
        }
        ZoneId zona = idoso.zona();
        Instant de = inicio.atStartOfDay(zona).toInstant();
        Instant ate = fim.plusDays(1).atStartOfDay(zona).toInstant();
        Instant agora = relogio.agora();
        if (ate.isAfter(agora)) {
            ate = agora;
        }
        List<DoseProgramada> doses = medicamentoId == null
                ? doseRepository.findPorIdosoEPeriodo(idoso.getId(), de, ate)
                : doseRepository.findPorIdosoMedicamentoEPeriodo(idoso.getId(), medicamentoId, de, ate);
        List<DoseDTO> lista = toDTOs(doses, true);
        Collections.reverse(lista);
        return lista;
    }

    @Transactional(readOnly = true)
    public List<DoseDTO> agenda(Usuario idoso) {
        ZoneId zona = idoso.zona();
        Instant agora = relogio.agora();
        Instant de = agora.atZone(zona).toLocalDate().atStartOfDay(zona).toInstant();
        Instant ate = agora.plus(Duration.ofDays(GeradorDoses.HORIZONTE_DIAS));
        return toDTOs(doseRepository.findPorIdosoEPeriodo(idoso.getId(), de, ate), false);
    }

    @Transactional(readOnly = true)
    public DoseDTO detalhe(UsuarioAutenticado u, Long doseId) {
        DoseProgramada dose = doseRepository.findById(doseId)
                .orElseThrow(() -> ApiException.naoEncontrado("Dose não encontrada."));
        autorizacao.exigirAcessoADose(u, dose);
        return toDTO(dose, u.isCuidador());
    }

    @Transactional(readOnly = true)
    public DoseDTO toDTO(DoseProgramada dose, boolean comNotificacoes) {
        return toDTOs(List.of(dose), comNotificacoes).get(0);
    }

    List<DoseDTO> toDTOs(List<DoseProgramada> doses, boolean comNotificacoes) {
        if (doses.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<Long> ids = doses.stream().map(DoseProgramada::getId).toList();
        Map<Long, ConfirmacaoDose> confirmacoes = confirmacaoRepository.findByDoseIds(ids).stream()
                .collect(Collectors.toMap(c -> c.getDose().getId(), Function.identity()));
        Map<Long, List<Notificacao>> notificacoes = comNotificacoes
                ? notificacaoRepository.findByDoseIds(ids).stream()
                .collect(Collectors.groupingBy(n -> n.getDose().getId()))
                : Map.of();
        return doses.stream().map(d -> {
            ConfirmacaoDose c = confirmacoes.get(d.getId());
            ConfirmacaoDTO cdto = c == null ? null : new ConfirmacaoDTO(c.getConfirmadoEm(), c.getHorarioAparelho(),
                    c.getUsuario() == null ? "(conta excluída)" : c.getUsuario().getNome(), c.isPeloCuidador());
            List<NotificacaoDTO> ndto = notificacoes.getOrDefault(d.getId(), List.of()).stream()
                    .map(NotificacaoService::toDTO).toList();
            Medicamento m = d.getMedicamento();
            return new DoseDTO(d.getId(), m.getIdoso().getId(), m.getIdoso().getNome(), m.getId(), m.getNome(),
                    m.getDosagem(), m.getOrientacoes(), d.getDataHoraPrevista(),
                    d.getHorario().getToleranciaMinutos(), d.getEstado(), cdto, ndto);
        }).collect(Collectors.toCollection(java.util.ArrayList::new));
    }
}
