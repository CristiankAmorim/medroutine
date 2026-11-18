package br.ucsal.medroutine.domain;

import java.time.Instant;

public interface Relogio {
    Instant agora();

    static Relogio sistema() {
        return Instant::now;
    }

    static Relogio fixo(Instant instante) {
        return () -> instante;
    }
}
