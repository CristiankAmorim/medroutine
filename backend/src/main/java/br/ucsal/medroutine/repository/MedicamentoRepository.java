package br.ucsal.medroutine.repository;

import br.ucsal.medroutine.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
    List<Medicamento> findByIdosoIdOrderByAtivoDescNomeAsc(Long idosoId);

    List<Medicamento> findByAtivoTrue();
}
