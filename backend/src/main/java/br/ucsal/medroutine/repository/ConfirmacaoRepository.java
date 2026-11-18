package br.ucsal.medroutine.repository;

import br.ucsal.medroutine.model.ConfirmacaoDose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConfirmacaoRepository extends JpaRepository<ConfirmacaoDose, Long> {
    Optional<ConfirmacaoDose> findByDoseId(Long doseId);

    @Query("select c from ConfirmacaoDose c left join fetch c.usuario where c.dose.id in :doseIds")
    List<ConfirmacaoDose> findByDoseIds(Collection<Long> doseIds);
}
