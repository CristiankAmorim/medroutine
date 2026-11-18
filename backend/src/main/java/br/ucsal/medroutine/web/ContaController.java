package br.ucsal.medroutine.web;

import br.ucsal.medroutine.auth.UsuarioAutenticado;
import br.ucsal.medroutine.service.ContaService;
import br.ucsal.medroutine.web.dto.FcmTokenRequest;
import br.ucsal.medroutine.web.dto.UsuarioDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conta")
public class ContaController {
    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping
    public UsuarioDTO dados(@AuthenticationPrincipal UsuarioAutenticado u) {
        return contaService.dados(u);
    }

    @PutMapping("/token-aparelho")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void token(@AuthenticationPrincipal UsuarioAutenticado u, @RequestBody FcmTokenRequest req) {
        contaService.registrarTokenAparelho(u, req.token());
    }

    @PostMapping("/sair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(@AuthenticationPrincipal UsuarioAutenticado u) {
        contaService.registrarTokenAparelho(u, null);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioAutenticado u) {
        contaService.excluir(u);
    }
}
