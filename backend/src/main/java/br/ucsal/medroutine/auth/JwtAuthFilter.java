package br.ucsal.medroutine.auth;

import br.ucsal.medroutine.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            jwtService.validar(cabecalho.substring(7))
                    .filter(u -> usuarioRepository.existsById(u.id()))
                    .ifPresent(u -> {
                        var autenticacao = new UsernamePasswordAuthenticationToken(u, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + u.tipo().name())));
                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    });
        }
        chain.doFilter(request, response);
    }
}
