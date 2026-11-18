package br.ucsal.medroutine.web;

import br.ucsal.medroutine.auth.AuthService;
import br.ucsal.medroutine.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse cadastrar(@Valid @RequestBody CadastroRequest req) {
        return authService.cadastrarCuidador(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.senha());
    }

    @PostMapping("/recuperar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recuperar(@Valid @RequestBody RecuperarSenhaRequest req) {
        authService.solicitarRecuperacao(req.email());
    }

    @PostMapping("/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinir(@Valid @RequestBody RedefinirSenhaRequest req) {
        authService.redefinirSenha(req.email(), req.codigo(), req.novaSenha());
    }

    @PostMapping("/codigo-idoso")
    public TokenResponse entrarComCodigo(@Valid @RequestBody CodigoRequest req) {
        return authService.entrarComCodigo(req.codigo());
    }
}
