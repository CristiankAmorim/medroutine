package br.ucsal.medroutine.auth;

import br.ucsal.medroutine.model.TipoUsuario;
import br.ucsal.medroutine.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey chave;
    private final Duration validadeCuidador;
    private final Duration validadeIdoso;

    public JwtService(@Value("${medroutine.jwt.segredo}") String segredo,
                      @Value("${medroutine.jwt.validade-cuidador-dias}") long diasCuidador,
                      @Value("${medroutine.jwt.validade-idoso-dias}") long diasIdoso) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.validadeCuidador = Duration.ofDays(diasCuidador);
        this.validadeIdoso = Duration.ofDays(diasIdoso);
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        Duration validade = usuario.isIdoso() ? validadeIdoso : validadeCuidador;
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("tipo", usuario.getTipoUsuario().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(chave)
                .compact();
    }

    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new UsuarioAutenticado(Long.valueOf(claims.getSubject()),
                    TipoUsuario.valueOf(claims.get("tipo", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
