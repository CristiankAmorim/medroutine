package br.ucsal.medroutine.repository;

import br.ucsal.medroutine.model.Usuario;
import br.ucsal.medroutine.model.VinculoCuidadorIdoso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VinculoRepository extends JpaRepository<VinculoCuidadorIdoso, Long> {
    boolean existsByCuidadorIdAndIdosoIdAndAtivoTrue(Long cuidadorId, Long idosoId);

    Optional<VinculoCuidadorIdoso> findByCuidadorIdAndIdosoIdAndAtivoTrue(Long cuidadorId, Long idosoId);

    @Query("select v.idoso from VinculoCuidadorIdoso v where v.cuidador.id = :cuidadorId and v.ativo = true order by v.idoso.nome")
    List<Usuario> findIdososAtivos(Long cuidadorId);

    @Query("select v.cuidador from VinculoCuidadorIdoso v where v.idoso.id = :idosoId and v.ativo = true")
    List<Usuario> findCuidadoresAtivos(Long idosoId);

    long countByIdosoIdAndAtivoTrue(Long idosoId);
}
