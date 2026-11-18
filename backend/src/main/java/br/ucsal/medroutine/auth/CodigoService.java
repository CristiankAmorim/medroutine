package br.ucsal.medroutine.auth;

import br.ucsal.medroutine.domain.Relogio;
import br.ucsal.medroutine.model.CodigoAcesso;
import br.ucsal.medroutine.model.TipoCodigo;
import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.repository.CodigoAcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class CodigoService {
    public static final Duration VALIDADE_ACESSO_IDOSO = Duration.ofHours(24);
    public static final Duration VALIDADE_CONVITE = Duration.ofHours(24);
    public static final Duration VALIDADE_RECUPERACAO = Duration.ofMinutes(30);

    private static final String ALFABETO_CONVITE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();
    private final CodigoAcessoRepository repository;
    private final Relogio relogio;

    public CodigoService(CodigoAcessoRepository repository, Relogio relogio) {
        this.repository = repository;
        this.relogio = relogio;
    }

    public record CodigoGerado(String codigo, Instant expiraEm) { }

    @Transactional
    public CodigoGerado gerar(TipoCodigo tipo, Usuario usuario, Usuario criadoPor) {
        Instant agora = relogio.agora();
        repository.invalidarAnteriores(usuario.getId(), tipo, agora);
        String codigo;
        do {
            codigo = tipo == TipoCodigo.CONVITE_CUIDADOR ? alfanumerico(8) : numerico(6);
        } while (!repository.findValidos(hash(codigo), tipo, agora).isEmpty());
        Instant expira = agora.plus(switch (tipo) {
            case ACESSO_IDOSO -> VALIDADE_ACESSO_IDOSO;
            case CONVITE_CUIDADOR -> VALIDADE_CONVITE;
            case RECUPERACAO_SENHA -> VALIDADE_RECUPERACAO;
        });
        repository.save(new CodigoAcesso(hash(codigo), tipo, usuario, criadoPor, agora, expira));
        return new CodigoGerado(codigo, expira);
    }

    @Transactional
    public Optional<CodigoAcesso> consumir(String codigo, TipoCodigo tipo) {
        if (codigo == null) {
            return Optional.empty();
        }
        Instant agora = relogio.agora();
        String normalizado = codigo.trim().toUpperCase().replace(" ", "").replace("-", "");
        List<CodigoAcesso> validos = repository.findValidos(hash(normalizado), tipo, agora);
        if (validos.isEmpty()) {
            return Optional.empty();
        }
        CodigoAcesso c = validos.get(0);
        c.marcarUsado(agora);
        return Optional.of(c);
    }

    private String numerico(int tamanho) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tamanho; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String alfanumerico(int tamanho) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tamanho; i++) {
            sb.append(ALFABETO_CONVITE.charAt(random.nextInt(ALFABETO_CONVITE.length())));
        }
        return sb.toString();
    }

    static String hash(String codigo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
