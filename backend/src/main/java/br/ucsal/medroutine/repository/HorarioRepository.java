package br.ucsal.medroutine.repository;

import br.ucsal.medroutine.model.HorarioMedicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HorarioRepository extends JpaRepository<HorarioMedicacao, Long> {
    @Query("select h from HorarioMedicacao h join fetch h.medicamento m join fetch m.idoso "
            + "where h.ativo = true and m.ativo = true")
    List<HorarioMedicacao> findAtivosComMedicamento();
}
