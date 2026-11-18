package br.ucsal.medroutine.domain;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GeradorDosesTest {
    private static final ZoneId ZONA = ZoneId.of("America/Bahia");

    private Instant em(int ano, int mes, int dia, int hora, int minuto) {
        return ZonedDateTime.of(ano, mes, dia, hora, minuto, 0, 0, ZONA).toInstant();
    }

    @Test
    public void aCadaOitoHorasGeraTresDosesPorDia() {
        RegraHorario regra = RegraHorario.aCadaNHoras(LocalTime.of(6, 0), 8, 30);
        List<Instant> doses = GeradorDoses.gerar(regra, LocalDate.of(2026, 11, 2), null, ZONA,
                em(2026, 11, 2, 0, 0), em(2026, 11, 3, 0, 0));
        assertEquals(3, doses.size());
        assertEquals(em(2026, 11, 2, 6, 0), doses.get(0));
        assertEquals(em(2026, 11, 2, 14, 0), doses.get(1));
        assertEquals(em(2026, 11, 2, 22, 0), doses.get(2));
    }

    @Test
    public void horarioDiarioGeraSeteDosesNoHorizonte() {
        RegraHorario regra = RegraHorario.diaria(LocalTime.of(8, 0), 30);
        for (int hora : new int[]{7, 10}) {
            Relogio relogio = Relogio.fixo(em(2026, 11, 2, hora, 0));
            List<Instant> doses = GeradorDoses.gerarHorizonte(regra, LocalDate.of(2026, 11, 2), null, ZONA, relogio);
            assertEquals("cadastro às " + hora + "h", 7, doses.size());
        }
    }

    @Test
    public void diasDaSemanaGeraSomenteNosDiasSelecionados() {
        RegraHorario regra = RegraHorario.diasSemana(LocalTime.of(9, 0),
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), 30);
        List<Instant> doses = GeradorDoses.gerar(regra, LocalDate.of(2026, 11, 2), null, ZONA,
                em(2026, 11, 2, 0, 0), em(2026, 11, 9, 0, 0));
        assertEquals(3, doses.size());
        assertEquals(em(2026, 11, 4, 9, 0), doses.get(1));
    }

    @Test
    public void respeitaDatasDeInicioEFimDoTratamento() {
        RegraHorario regra = RegraHorario.diaria(LocalTime.of(8, 0), 30);
        List<Instant> doses = GeradorDoses.gerar(regra, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5), ZONA,
                em(2026, 11, 1, 0, 0), em(2026, 11, 10, 0, 0));
        assertEquals(3, doses.size());
        assertEquals(em(2026, 11, 3, 8, 0), doses.get(0));
        assertEquals(em(2026, 11, 5, 8, 0), doses.get(2));
    }

    @Test
    public void dosesSaoArmazenadasEmUtc() {
        RegraHorario regra = RegraHorario.diaria(LocalTime.of(8, 0), 30);
        List<Instant> doses = GeradorDoses.gerar(regra, LocalDate.of(2026, 11, 2), null, ZONA,
                em(2026, 11, 2, 0, 0), em(2026, 11, 3, 0, 0));
        assertEquals(Instant.parse("2026-11-02T11:00:00Z"), doses.get(0));
    }
}
