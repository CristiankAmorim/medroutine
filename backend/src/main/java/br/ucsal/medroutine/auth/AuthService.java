package br.ucsal.medroutine.auth;

import br.ucsal.medroutine.domain.Relogio;
import br.ucsal.medroutine.model.CodigoAcesso;
import br.ucsal.medroutine.model.TipoCodigo;
import br.ucsal.medroutine.model.TipoUsuario;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.repository.UsuarioRepository;
import br.ucsal.medroutine.service.ApiException;
import br.ucsal.medroutine.service.EmailService;
import br.ucsal.medroutine.web.dto.CadastroRequest;
import br.ucsal.medroutine.web.dto.TokenResponse;
import br.ucsal.medroutine.web.dto.UsuarioDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CodigoService codigoService;
    private final EmailService emailService;
    private final Relogio relogio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       CodigoService codigoService, EmailService emailService, Relogio relogio) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.codigoService = codigoService;
        this.emailService = emailService;
        this.relogio = relogio;
    }

    @Transactional
    public TokenResponse cadastrarCuidador(CadastroRequest req) {
        if (!req.consentimento()) {
            throw ApiException.requisicaoInvalida(
                    "Para usar o MedRoutine é preciso concordar com o tratamento dos dados de saúde.");
        }
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflito("Já existe uma conta com este e-mail.");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setTipoUsuario(TipoUsuario.CUIDADOR);
        u.setConsentimentoEm(relogio.agora());
        usuarioRepository.save(u);
        return new TokenResponse(jwtService.gerarToken(u), UsuarioDTO.de(u));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String senha) {
        Usuario u = usuarioRepository.findByEmailIgnoreCaseAndTipoUsuario(email.trim(), TipoUsuario.CUIDADOR)
                .filter(c -> passwordEncoder.matches(senha, c.getSenhaHash()))
                .orElseThrow(() -> ApiException.naoAutorizado("E-mail ou senha incorretos."));
        return new TokenResponse(jwtService.gerarToken(u), UsuarioDTO.de(u));
    }

    @Transactional
    public void solicitarRecuperacao(String email) {
        usuarioRepository.findByEmailIgnoreCaseAndTipoUsuario(email.trim(), TipoUsuario.CUIDADOR).ifPresent(u -> {
            CodigoService.CodigoGerado codigo = codigoService.gerar(TipoCodigo.RECUPERACAO_SENHA, u, u);
            emailService.enviarCodigoRecuperacao(u.getEmail(), u.getNome(), codigo.codigo());
        });
    }

    @Transactional
    public void redefinirSenha(String email, String codigo, String novaSenha) {
        CodigoAcesso c = codigoService.consumir(codigo, TipoCodigo.RECUPERACAO_SENHA)
                .filter(x -> x.getUsuario().getEmail().equalsIgnoreCase(email.trim()))
                .orElseThrow(() -> ApiException.requisicaoInvalida("Código inválido ou expirado."));
        c.getUsuario().setSenhaHash(passwordEncoder.encode(novaSenha));
    }

    @Transactional
    public TokenResponse entrarComCodigo(String codigo) {
        CodigoAcesso c = codigoService.consumir(codigo, TipoCodigo.ACESSO_IDOSO)
                .orElseThrow(() -> ApiException.requisicaoInvalida(
                        "Código inválido, já utilizado ou expirado. Peça um novo código ao seu cuidador."));
        Usuario idoso = c.getUsuario();
        return new TokenResponse(jwtService.gerarToken(idoso), UsuarioDTO.de(idoso));
    }
}
