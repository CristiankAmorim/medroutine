package br.ucsal.medroutine.domain;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RegrasDoseTest {
    private static final Instant PREVISTO = Instant.parse("2026-11-02T15:00:00Z");

    private Instant mais(long minutos) {
        return PREVISTO.plus(Duration.ofMinutes(minutos));
    }

    @Test
    public void confirmacaoDentroDaToleranciaFicaConfirmada() {
        assertEquals(EstadoDose.CONFIRMADA,
                RegrasDose.estadoAposConfirmacao(EstadoDose.PENDENTE, PREVISTO, 30, mais(10)));
    }

    @Test
    public void confirmacaoAposAlertaFicaComAtraso() {
        assertEquals(EstadoDose.CONFIRMADA_COM_ATRASO,
                RegrasDose.estadoAposConfirmacao(EstadoDose.NAO_CONFIRMADA, PREVISTO, 30, mais(45)));
    }

    @Test
    public void confirmacaoAposToleranciaAntesDoProcessamentoFicaComAtraso() {
        assertEquals(EstadoDose.CONFIRMADA_COM_ATRASO,
                RegrasDose.estadoAposConfirmacao(EstadoDose.PENDENTE, PREVISTO, 30, mais(31)));
    }

    @Test
    public void confirmacaoOfflineDentroDaToleranciaFicaConfirmadaMesmoAposAlerta() {
        Instant momento = RegrasDose.momentoDaConfirmacao(PREVISTO, mais(40), mais(1));
        assertEquals(mais(1), momento);
        assertEquals(EstadoDose.CONFIRMADA,
                RegrasDose.estadoAposConfirmacao(EstadoDose.NAO_CONFIRMADA, PREVISTO, 5, momento));
    }

    @Test
    public void horarioDoAparelhoImplausivelEhIgnorado() {
        assertEquals(mais(10), RegrasDose.momentoDaConfirmacao(PREVISTO, mais(10), mais(20)));
        assertEquals(mais(10), RegrasDose.momentoDaConfirmacao(PREVISTO, mais(10), mais(-90)));
        assertEquals(mais(10), RegrasDose.momentoDaConfirmacao(PREVISTO, mais(10), null));
    }

    @Test(expected = DoseJaConfirmadaException.class)
    public void segundaConfirmacaoERecusada() {
        RegrasDose.estadoAposConfirmacao(EstadoDose.CONFIRMADA, PREVISTO, 30, mais(5));
    }

    @Test
    public void confirmacaoAceitaAPartirDeSessentaMinutosAntes() {
        assertTrue(RegrasDose.podeConfirmar(PREVISTO, mais(-60)));
        assertFalse(RegrasDose.podeConfirmar(PREVISTO, mais(-61)));
        try {
            RegrasDose.estadoAposConfirmacao(EstadoDose.PENDENTE, PREVISTO, 30, mais(-61));
            fail("deveria recusar a confirmação antecipada");
        } catch (ValidacaoException esperado) {
            assertEquals("dose", esperado.getCampo());
        }
    }

    @Test
    public void alertaSomenteAposATolerancia() {
        assertFalse(RegrasDose.deveAlertar(EstadoDose.PENDENTE, PREVISTO, 5, mais(4)));
        assertFalse(RegrasDose.deveAlertar(EstadoDose.PENDENTE, PREVISTO, 5, mais(5)));
        assertTrue(RegrasDose.deveAlertar(EstadoDose.PENDENTE, PREVISTO, 5, mais(5).plusSeconds(1)));
    }

    @Test
    public void doseJaAlertadaOuConfirmadaNaoGeraNovoAlerta() {
        assertFalse(RegrasDose.deveAlertar(EstadoDose.NAO_CONFIRMADA, PREVISTO, 5, mais(60)));
        assertFalse(RegrasDose.deveAlertar(EstadoDose.CONFIRMADA, PREVISTO, 5, mais(60)));
        assertFalse(RegrasDose.deveAlertar(EstadoDose.CONFIRMADA_COM_ATRASO, PREVISTO, 5, mais(60)));
    }
}
