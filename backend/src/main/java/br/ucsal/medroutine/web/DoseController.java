package br.ucsal.medroutine.web;

import br.ucsal.medroutine.auth.AutorizacaoService;
import br.ucsal.medroutine.auth.UsuarioAutenticado;
import br.ucsal.medroutine.service.ConfirmacaoService;
import br.ucsal.medroutine.service.DoseService;
import br.ucsal.medroutine.web.dto.AgendaDTO;
import br.ucsal.medroutine.web.dto.ConfirmacaoResponse;
import br.ucsal.medroutine.web.dto.ConfirmarRequest;
import br.ucsal.medroutine.web.dto.DoseDTO;
import br.ucsal.medroutine.web.dto.UsuarioDTO;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.domain.Relogio;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api")
public class DoseController {
    private final DoseService doseService;
    private final ConfirmacaoService confirmacaoService;
    private final AutorizacaoService autorizacao;
    private final Relogio relogio;

    public DoseController(DoseService doseService, ConfirmacaoService confirmacaoService,
                          AutorizacaoService autorizacao, Relogio relogio) {
        this.doseService = doseService;
        this.confirmacaoService = confirmacaoService;
        this.autorizacao = autorizacao;
        this.relogio = relogio;
    }

    @GetMapping("/doses/{doseId}")
    public DoseDTO dose(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long doseId) {
        return doseService.detalhe(u, doseId);
    }

    @PostMapping("/doses/{doseId}/confirmacao")
    public ConfirmacaoResponse confirmar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long doseId,
                                         @RequestBody(required = false) ConfirmarRequest req) {
        return confirmacaoService.confirmar(u, doseId, req == null ? null : req.horarioAparelho());
    }

    @GetMapping("/eu/agenda")
    public AgendaDTO agenda(@AuthenticationPrincipal UsuarioAutenticado u) {
        Usuario idoso = autorizacao.carregar(u);
        return new AgendaDTO(UsuarioDTO.de(idoso), relogio.agora(), doseService.agenda(idoso));
    }
}
