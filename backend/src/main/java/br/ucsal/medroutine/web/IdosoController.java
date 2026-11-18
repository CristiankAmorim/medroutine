package br.ucsal.medroutine.web;

import br.ucsal.medroutine.auth.AutorizacaoService;
import br.ucsal.medroutine.auth.UsuarioAutenticado;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.service.DoseService;
import br.ucsal.medroutine.service.IdosoService;
import br.ucsal.medroutine.service.MedicamentoService;
import br.ucsal.medroutine.service.NotificacaoService;
import br.ucsal.medroutine.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class IdosoController {
    private final IdosoService idosoService;
    private final MedicamentoService medicamentoService;
    private final DoseService doseService;
    private final NotificacaoService notificacaoService;
    private final AutorizacaoService autorizacao;

    public IdosoController(IdosoService idosoService, MedicamentoService medicamentoService, DoseService doseService,
                           NotificacaoService notificacaoService, AutorizacaoService autorizacao) {
        this.idosoService = idosoService;
        this.medicamentoService = medicamentoService;
        this.doseService = doseService;
        this.notificacaoService = notificacaoService;
        this.autorizacao = autorizacao;
    }

    @GetMapping("/idosos")
    public List<IdosoDTO> listar(@AuthenticationPrincipal UsuarioAutenticado u) {
        return idosoService.listar(u);
    }

    @PostMapping("/idosos")
    @ResponseStatus(HttpStatus.CREATED)
    public NovoIdosoResponse criar(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody IdosoRequest req) {
        return idosoService.criar(u, req);
    }

    @PutMapping("/idosos/{idosoId}")
    public IdosoDTO atualizar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId,
                              @Valid @RequestBody IdosoRequest req) {
        IdosoDTO dto = idosoService.atualizar(u, idosoId, req);
        notificacaoService.avisarAgendaAtualizada(idosoId);
        return dto;
    }

    @DeleteMapping("/idosos/{idosoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId) {
        idosoService.excluir(u, idosoId);
    }

    @PostMapping("/idosos/{idosoId}/codigo-acesso")
    public CodigoDTO codigoAcesso(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId) {
        return idosoService.gerarCodigoAcesso(u, idosoId);
    }

    @PostMapping("/idosos/{idosoId}/convites")
    public CodigoDTO convite(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId) {
        return idosoService.gerarConvite(u, idosoId);
    }

    @PostMapping("/vinculos/aceitar")
    public IdosoDTO aceitarConvite(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody CodigoRequest req) {
        return idosoService.aceitarConvite(u, req.codigo());
    }

    @DeleteMapping("/idosos/{idosoId}/vinculo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincular(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId) {
        idosoService.desvincular(u, idosoId);
    }

    @GetMapping("/idosos/{idosoId}/medicamentos")
    public List<MedicamentoDTO> medicamentos(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId) {
        return medicamentoService.listar(autorizacao.exigirIdosoVinculado(u, idosoId));
    }

    @PostMapping("/idosos/{idosoId}/medicamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentoDTO criarMedicamento(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId,
                                           @RequestBody MedicamentoRequest req) {
        MedicamentoDTO dto = medicamentoService.criar(autorizacao.exigirIdosoVinculado(u, idosoId), req);
        notificacaoService.avisarAgendaAtualizada(idosoId);
        return dto;
    }

    @GetMapping("/medicamentos/{medicamentoId}")
    public MedicamentoDTO medicamento(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long medicamentoId) {
        return medicamentoService.buscar(u, medicamentoId);
    }

    @PutMapping("/medicamentos/{medicamentoId}")
    public MedicamentoDTO atualizarMedicamento(@AuthenticationPrincipal UsuarioAutenticado u,
                                               @PathVariable Long medicamentoId, @RequestBody MedicamentoRequest req) {
        MedicamentoDTO dto = medicamentoService.atualizar(u, medicamentoId, req);
        notificacaoService.avisarAgendaAtualizada(dto.idosoId());
        return dto;
    }

    @PostMapping("/medicamentos/{medicamentoId}/desativar")
    public MedicamentoDTO desativarMedicamento(@AuthenticationPrincipal UsuarioAutenticado u,
                                               @PathVariable Long medicamentoId) {
        MedicamentoDTO dto = medicamentoService.desativar(u, medicamentoId);
        notificacaoService.avisarAgendaAtualizada(dto.idosoId());
        return dto;
    }

    @GetMapping("/idosos/{idosoId}/doses")
    public List<DoseDTO> dosesDoDia(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                    LocalDate data) {
        Usuario idoso = autorizacao.exigirIdosoVinculado(u, idosoId);
        LocalDate dia = data != null ? data : LocalDate.now(idoso.zona());
        return doseService.dosesDoDia(idoso, dia);
    }

    @GetMapping("/idosos/{idosoId}/historico")
    public List<DoseDTO> historico(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long idosoId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                   @RequestParam(required = false) Long medicamentoId) {
        return doseService.historico(autorizacao.exigirIdosoVinculado(u, idosoId), inicio, fim, medicamentoId);
    }

    @GetMapping("/notificacoes")
    public List<NotificacaoDTO> notificacoes(@AuthenticationPrincipal UsuarioAutenticado u) {
        return notificacaoService.listarDoCuidador(u.id());
    }
}
