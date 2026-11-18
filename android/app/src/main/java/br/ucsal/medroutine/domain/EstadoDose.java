package br.ucsal.medroutine.domain;

public enum EstadoDose {
    PENDENTE,
    CONFIRMADA,
    NAO_CONFIRMADA,
    CONFIRMADA_COM_ATRASO;

    public boolean isConfirmada() {
        return this == CONFIRMADA || this == CONFIRMADA_COM_ATRASO;
    }
}
