package br.ucsal.medroutine.service;

import br.ucsal.medroutine.auth.AutorizacaoService;
import br.ucsal.medroutine.auth.UsuarioAutenticado;
import br.ucsal.medroutine.domain.RegraHorario;
import br.ucsal.medroutine.domain.Relogio;
import br.ucsal.medroutine.domain.TipoFrequencia;
import br.ucsal.medroutine.domain.ValidadorTratamento;
import br.ucsal.medroutine.domain.ValidacaoException;
import br.ucsal.medroutine.model.HorarioMedicacao;
import br.ucsal.medroutine.model.Medicamento;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.repository.DoseRepository;
import br.ucsal.medroutine.repository.HorarioRepository;
import br.ucsal.medroutine.repository.MedicamentoRepository;
import br.ucsal.medroutine.web.dto.HorarioDTO;
import br.ucsal.medroutine.web.dto.HorarioRequest;
import br.ucsal.medroutine.web.dto.MedicamentoDTO;
import br.ucsal.medroutine.web.dto.MedicamentoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class MedicamentoService {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final MedicamentoRepository medicamentoRepository;
    private final HorarioRepository horarioRepository;
    private final DoseRepository doseRepository;
    private final DoseService doseService;
    private final AutorizacaoService autorizacao;
    private final Relogio relogio;

    public MedicamentoService(MedicamentoRepository medicamentoRepository, HorarioRepository horarioRepository,
                              DoseRepository doseRepository, DoseService doseService,
                              AutorizacaoService autorizacao, Relogio relogio) {
        this.autorizacao = autorizacao;
        this.medicamentoRepository = medicamentoRepository;
        this.horarioRepository = horarioRepository;
        this.doseRepository = doseRepository;
        this.doseService = doseService;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<MedicamentoDTO> listar(Usuario idoso) {
        return medicamentoRepository.findByIdosoIdOrderByAtivoDescNomeAsc(idoso.getId()).stream()
                .map(MedicamentoService::toDTO).toList();
    }

    @Transactional
    public MedicamentoDTO criar(Usuario idoso, MedicamentoRequest req) {
        List<RegraHorario> regras = validar(req);
        Medicamento m = new Medicamento(idoso);
        preencher(m, req);
        medicamentoRepository.save(m);
        adicionarHorarios(m, regras);
        return toDTO(m);
    }

    @Transactional(readOnly = true)
    public MedicamentoDTO buscar(UsuarioAutenticado u, Long medicamentoId) {
        return toDTO(carregar(u, medicamentoId));
    }

    @Transactional
    public MedicamentoDTO atualizar(UsuarioAutenticado u, Long medicamentoId, MedicamentoRequest req) {
        Medicamento m = carregar(u, medicamentoId);
        if (!m.isAtivo()) {
            throw ApiException.conflito("Reative o medicamento antes de editá-lo.");
        }
        List<RegraHorario> regras = validar(req);
        preencher(m, req);
        desativarHorarios(m);
        adicionarHorarios(m, regras);
        return toDTO(m);
    }

    @Transactional
    public MedicamentoDTO desativar(UsuarioAutenticado u, Long medicamentoId) {
        Medicamento m = carregar(u, medicamentoId);
        m.setAtivo(false);
        desativarHorarios(m);
        return toDTO(m);
    }

    private Medicamento carregar(UsuarioAutenticado u, Long medicamentoId) {
        Medicamento m = medicamentoRepository.findById(medicamentoId)
                .orElseThrow(() -> ApiException.naoEncontrado("Medicamento não encontrado."));
        autorizacao.exigirIdosoVinculado(u, m.getIdoso().getId());
        return m;
    }

    private void desativarHorarios(Medicamento m) {
        for (HorarioMedicacao h : m.getHorarios()) {
            if (h.isAtivo()) {
                h.setAtivo(false);
                doseRepository.removerPendentesFuturas(h.getId(), relogio.agora());
            }
        }
    }

    private void adicionarHorarios(Medicamento m, List<RegraHorario> regras) {
        for (RegraHorario regra : regras) {
            HorarioMedicacao h = horarioRepository.save(new HorarioMedicacao(m, regra));
            m.getHorarios().add(h);
            doseService.gerarDoses(h);
        }
    }

    private void preencher(Medicamento m, MedicamentoRequest req) {
        m.setNome(req.nome().trim());
        m.setDosagem(req.dosagem().trim());
        m.setOrientacoes(req.orientacoes() == null || req.orientacoes().isBlank() ? null : req.orientacoes().trim());
        m.setDataInicio(req.dataInicio());
        m.setDataFim(req.dataFim());
    }

    private List<RegraHorario> validar(MedicamentoRequest req) {
        ValidadorTratamento.validarMedicamento(req.nome(), req.dosagem(), req.dataInicio(), req.dataFim());
        if (req.orientacoes() != null && req.orientacoes().length() > 500) {
            throw new ValidacaoException("orientacoes", "As orientações devem ter no máximo 500 caracteres.");
        }
        if (req.horarios() == null || req.horarios().isEmpty()) {
            throw new ValidacaoException("horarios", "Adicione ao menos um horário.");
        }
        List<RegraHorario> regras = new ArrayList<>();
        for (HorarioRequest h : req.horarios()) {
            EnumSet<DayOfWeek> dias = h.diasSemana() == null || h.diasSemana().isEmpty()
                    ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(h.diasSemana());
            RegraHorario regra = new RegraHorario(ValidadorTratamento.parseHora(h.hora()), h.tipoFrequencia(),
                    h.intervaloHoras(), dias, ValidadorTratamento.toleranciaOuPadrao(h.toleranciaMinutos()));
            ValidadorTratamento.validarHorario(regra);
            regras.add(regra);
        }
        return regras;
    }

    static MedicamentoDTO toDTO(Medicamento m) {
        List<HorarioDTO> horarios = m.getHorarios().stream()
                .filter(HorarioMedicacao::isAtivo)
                .map(h -> new HorarioDTO(h.getId(), HH_MM.format(h.getHora()), h.getTipoFrequencia(),
                        h.getTipoFrequencia() == TipoFrequencia.A_CADA_N_HORAS ? h.getIntervaloHoras() : null,
                        new ArrayList<>(h.getDiasSemanaSet()), h.getToleranciaMinutos()))
                .toList();
        return new MedicamentoDTO(m.getId(), m.getIdoso().getId(), m.getNome(), m.getDosagem(), m.getOrientacoes(),
                m.getDataInicio(), m.getDataFim(), m.isAtivo(), horarios);
    }
}
