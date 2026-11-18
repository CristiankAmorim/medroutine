package br.ucsal.medroutine.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class GeradorDoses {
    public static final int HORIZONTE_DIAS = 7;

    private static final int SEGUNDOS_DIA = 24 * 60 * 60;

    private GeradorDoses() { }

    public static List<Instant> gerar(RegraHorario regra, LocalDate inicioTratamento, LocalDate fimTratamento,
                                      ZoneId zona, Instant de, Instant ate) {
        List<Instant> resultado = new ArrayList<>();
        LocalDate dia = de.atZone(zona).toLocalDate();
        if (inicioTratamento != null && dia.isBefore(inicioTratamento)) {
            dia = inicioTratamento;
        }
        LocalDate ultimoDia = ate.atZone(zona).toLocalDate();
        if (fimTratamento != null && fimTratamento.isBefore(ultimoDia)) {
            ultimoDia = fimTratamento;
        }
        while (!dia.isAfter(ultimoDia)) {
            for (LocalTime hora : horasDoDia(regra, dia)) {
                Instant previsto = ZonedDateTime.of(dia, hora, zona).toInstant();
                if (!previsto.isBefore(de) && previsto.isBefore(ate)) {
                    resultado.add(previsto);
                }
            }
            dia = dia.plusDays(1);
        }
        return resultado;
    }

    public static List<Instant> gerarHorizonte(RegraHorario regra, LocalDate inicioTratamento,
                                               LocalDate fimTratamento, ZoneId zona, Relogio relogio) {
        Instant agora = relogio.agora();
        return gerar(regra, inicioTratamento, fimTratamento, zona, agora,
                agora.plusSeconds((long) HORIZONTE_DIAS * SEGUNDOS_DIA));
    }

    static List<LocalTime> horasDoDia(RegraHorario regra, LocalDate dia) {
        List<LocalTime> horas = new ArrayList<>();
        switch (regra.getTipoFrequencia()) {
            case DIARIA:
                horas.add(regra.getHora());
                break;
            case DIAS_SEMANA:
                if (regra.getDiasSemana().contains(dia.getDayOfWeek())) {
                    horas.add(regra.getHora());
                }
                break;
            case A_CADA_N_HORAS:
                int passo = regra.getIntervaloHoras() * 3600;
                for (int s = regra.getHora().toSecondOfDay(); s < SEGUNDOS_DIA; s += passo) {
                    horas.add(LocalTime.ofSecondOfDay(s));
                }
                break;
            default:
                break;
        }
        return horas;
    }
}
