package br.ucsal.medroutine.web.dto;

import java.time.LocalDate;
import java.util.List;

public record MedicamentoDTO(Long id, Long idosoId, String nome, String dosagem, String orientacoes,
                             LocalDate dataInicio, LocalDate dataFim, boolean ativo, List<HorarioDTO> horarios) { }
