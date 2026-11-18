package br.ucsal.medroutine.domain;

public class DoseJaConfirmadaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DoseJaConfirmadaException() {
        super("Esta dose já foi confirmada.");
    }
}
