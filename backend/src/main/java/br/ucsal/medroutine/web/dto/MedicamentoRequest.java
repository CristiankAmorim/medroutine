package br.ucsal.medroutine.web.dto;

import java.time.LocalDate;
import java.util.List;

public record MedicamentoRequest(String nome, String dosagem, String orientacoes,
                                 LocalDate dataInicio, LocalDate dataFim, List<HorarioRequest> horarios) { }
