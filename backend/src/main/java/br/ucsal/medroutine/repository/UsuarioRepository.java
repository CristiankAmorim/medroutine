package br.ucsal.medroutine.repository;

import br.ucsal.medroutine.model.TipoUsuario;
import br.ucsal.medroutine.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCaseAndTipoUsuario(String email, TipoUsuario tipoUsuario);

    boolean existsByEmailIgnoreCase(String email);
}
