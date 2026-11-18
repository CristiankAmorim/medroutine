package br.ucsal.medroutine.domain;

import java.time.Duration;
import java.time.Instant;

public final class RegrasDose {
    public static final Duration ANTECEDENCIA_CONFIRMACAO = Duration.ofMinutes(60);

    public static final Duration ADIAMENTO_LEMBRETE = Duration.ofMinutes(10);

    private RegrasDose() { }

    public static Instant limiteTolerancia(Instant previsto, int toleranciaMinutos) {
        return previsto.plus(Duration.ofMinutes(toleranciaMinutos));
    }

    public static boolean podeConfirmar(Instant previsto, Instant agora) {
        return !agora.isBefore(previsto.minus(ANTECEDENCIA_CONFIRMACAO));
    }

    public static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(2);

    public static Instant momentoDaConfirmacao(Instant previsto, Instant servidor, Instant aparelho) {
        if (aparelho == null || aparelho.isAfter(servidor.plus(TOLERANCIA_RELOGIO))
                || !podeConfirmar(previsto, aparelho)) {
            return servidor;
        }
        return aparelho.isBefore(servidor) ? aparelho : servidor;
    }

    public static EstadoDose estadoAposConfirmacao(EstadoDose atual, Instant previsto,
                                                   int toleranciaMinutos, Instant agora) {
        if (atual.isConfirmada()) {
            throw new DoseJaConfirmadaException();
        }
        if (!podeConfirmar(previsto, agora)) {
            throw new ValidacaoException("dose",
                    "Esta dose só pode ser confirmada a partir de 60 minutos antes do horário.");
        }
        if (agora.isAfter(limiteTolerancia(previsto, toleranciaMinutos))) {
            return EstadoDose.CONFIRMADA_COM_ATRASO;
        }
        return EstadoDose.CONFIRMADA;
    }

    public static boolean deveAlertar(EstadoDose estado, Instant previsto, int toleranciaMinutos, Instant agora) {
        return estado == EstadoDose.PENDENTE && agora.isAfter(limiteTolerancia(previsto, toleranciaMinutos));
    }
}
