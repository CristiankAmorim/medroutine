package br.ucsal.medroutine.repository;

import br.ucsal.medroutine.model.Notificacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    @Query("select n from Notificacao n join fetch n.destinatario join fetch n.dose d join fetch d.horario h "
            + "join fetch h.medicamento m join fetch m.idoso where n.id = :id")
    Optional<Notificacao> findCompleta(Long id);

    @Query("select n from Notificacao n join fetch n.destinatario where n.dose.id in :doseIds order by n.criadoEm")
    List<Notificacao> findByDoseIds(Collection<Long> doseIds);

    @Query("select n from Notificacao n join fetch n.dose d join fetch d.horario h join fetch h.medicamento m "
            + "join fetch m.idoso i where n.destinatario.id = :cuidadorId and exists ("
            + "select v from VinculoCuidadorIdoso v where v.cuidador.id = :cuidadorId and v.idoso = i and v.ativo = true) "
            + "order by n.criadoEm desc")
    List<Notificacao> findDoCuidador(Long cuidadorId, Pageable pageable);
}
