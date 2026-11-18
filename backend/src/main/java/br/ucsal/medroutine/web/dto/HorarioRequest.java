package br.ucsal.medroutine.web.dto;

import br.ucsal.medroutine.domain.TipoFrequencia;

import java.time.DayOfWeek;
import java.util.List;

public record HorarioRequest(String hora, TipoFrequencia tipoFrequencia, Integer intervaloHoras,
                             List<DayOfWeek> diasSemana, Integer toleranciaMinutos) { }
