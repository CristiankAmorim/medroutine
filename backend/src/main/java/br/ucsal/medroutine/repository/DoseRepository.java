package br.ucsal.medroutine.repository;

import br.ucsal.medroutine.model.DoseProgramada;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DoseRepository extends JpaRepository<DoseProgramada, Long> {
    boolean existsByHorarioIdAndDataHoraPrevista(Long horarioId, Instant dataHoraPrevista);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DoseProgramada d where d.id = :id")
    Optional<DoseProgramada> findByIdParaAtualizar(Long id);

    @Query("select d from DoseProgramada d join fetch d.horario h join fetch h.medicamento m "
            + "where m.idoso.id = :idosoId and d.dataHoraPrevista >= :de and d.dataHoraPrevista < :ate "
            + "order by d.dataHoraPrevista")
    List<DoseProgramada> findPorIdosoEPeriodo(Long idosoId, Instant de, Instant ate);

    @Query("select d from DoseProgramada d join fetch d.horario h join fetch h.medicamento m "
            + "where m.idoso.id = :idosoId and m.id = :medicamentoId "
            + "and d.dataHoraPrevista >= :de and d.dataHoraPrevista < :ate "
            + "order by d.dataHoraPrevista")
    List<DoseProgramada> findPorIdosoMedicamentoEPeriodo(Long idosoId, Long medicamentoId, Instant de, Instant ate);

    @Modifying
    @Query("delete from DoseProgramada d where d.horario.id = :horarioId "
            + "and d.estado = br.ucsal.medroutine.domain.EstadoDose.PENDENTE and d.dataHoraPrevista > :agora")
    int removerPendentesFuturas(Long horarioId, Instant agora);

    @Query(value = "select d.id from dose_programada d join horario_medicacao h on h.id = d.horario_id "
            + "where d.estado = 'PENDENTE' "
            + "and d.data_hora_prevista + make_interval(mins => h.tolerancia_minutos) < :agora "
            + "order by d.data_hora_prevista", nativeQuery = true)
    List<Long> findIdsComToleranciaExcedida(Instant agora);

    @Modifying
    @Query("update DoseProgramada d set d.estado = br.ucsal.medroutine.domain.EstadoDose.NAO_CONFIRMADA "
            + "where d.id = :id and d.estado = br.ucsal.medroutine.domain.EstadoDose.PENDENTE")
    int marcarNaoConfirmada(Long id);
}
