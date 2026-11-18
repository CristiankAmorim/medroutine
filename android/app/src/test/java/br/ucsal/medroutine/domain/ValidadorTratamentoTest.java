package br.ucsal.medroutine.domain;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ValidadorTratamentoTest {
    @Test
    public void toleranciaPadraoELimites() {
        assertEquals(30, ValidadorTratamento.toleranciaOuPadrao(null));
        assertEquals(5, ValidadorTratamento.toleranciaOuPadrao(5));
        assertEquals(240, ValidadorTratamento.toleranciaOuPadrao(240));
        for (int invalido : new int[]{4, 241, 0, -10}) {
            try {
                ValidadorTratamento.toleranciaOuPadrao(invalido);
                fail("deveria recusar " + invalido);
            } catch (ValidacaoException e) {
                assertEquals("toleranciaMinutos", e.getCampo());
            }
        }
    }

    @Test
    public void terminoAnteriorAoInicioERecusado() {
        try {
            ValidadorTratamento.validarMedicamento("Losartana", "50 mg",
                    LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9));
            fail();
        } catch (ValidacaoException e) {
            assertEquals("dataFim", e.getCampo());
        }
    }

    @Test
    public void horariosInexistentesSaoRecusados() {
        assertEquals(LocalTime.of(8, 30), ValidadorTratamento.parseHora("08:30"));
        for (String invalido : new String[]{"25:00", "10:75", "8h", ""}) {
            try {
                ValidadorTratamento.parseHora(invalido);
                fail("deveria recusar " + invalido);
            } catch (ValidacaoException e) {
                assertEquals("hora", e.getCampo());
            }
        }
    }

    @Test
    public void intervaloEDiasSaoValidados() {
        try {
            ValidadorTratamento.validarHorario(RegraHorario.aCadaNHoras(LocalTime.of(6, 0), 0, 30));
            fail();
        } catch (ValidacaoException e) {
            assertEquals("intervaloHoras", e.getCampo());
        }
        try {
            ValidadorTratamento.validarHorario(RegraHorario.diasSemana(LocalTime.of(6, 0),
                    Collections.emptySet(), 30));
            fail();
        } catch (ValidacaoException e) {
            assertEquals("diasSemana", e.getCampo());
        }
    }
}
