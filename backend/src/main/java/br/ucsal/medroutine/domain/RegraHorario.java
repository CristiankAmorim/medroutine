package br.ucsal.medroutine.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class RegraHorario {
    private final LocalTime hora;
    private final TipoFrequencia tipoFrequencia;
    private final Integer intervaloHoras;
    private final Set<DayOfWeek> diasSemana;
    private final int toleranciaMinutos;

    public RegraHorario(LocalTime hora, TipoFrequencia tipoFrequencia, Integer intervaloHoras,
                        Set<DayOfWeek> diasSemana, int toleranciaMinutos) {
        this.hora = hora;
        this.tipoFrequencia = tipoFrequencia;
        this.intervaloHoras = intervaloHoras;
        this.diasSemana = diasSemana == null || diasSemana.isEmpty()
                ? Collections.<DayOfWeek>emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(diasSemana));
        this.toleranciaMinutos = toleranciaMinutos;
    }

    public static RegraHorario diaria(LocalTime hora, int toleranciaMinutos) {
        return new RegraHorario(hora, TipoFrequencia.DIARIA, null, null, toleranciaMinutos);
    }

    public static RegraHorario aCadaNHoras(LocalTime inicio, int intervaloHoras, int toleranciaMinutos) {
        return new RegraHorario(inicio, TipoFrequencia.A_CADA_N_HORAS, intervaloHoras, null, toleranciaMinutos);
    }

    public static RegraHorario diasSemana(LocalTime hora, Set<DayOfWeek> dias, int toleranciaMinutos) {
        return new RegraHorario(hora, TipoFrequencia.DIAS_SEMANA, null, dias, toleranciaMinutos);
    }

    public LocalTime getHora() { return hora; }
    public TipoFrequencia getTipoFrequencia() { return tipoFrequencia; }
    public Integer getIntervaloHoras() { return intervaloHoras; }
    public Set<DayOfWeek> getDiasSemana() { return diasSemana; }
    public int getToleranciaMinutos() { return toleranciaMinutos; }
}
