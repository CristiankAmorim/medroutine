package br.ucsal.medroutine.data.model;

import java.time.Instant;

public class Codigo {
    public String codigo;
    public Instant expiraEm;

    public Codigo() { }

    public Codigo(String codigo, Instant expiraEm) {
        this.codigo = codigo;
        this.expiraEm = expiraEm;
    }
}
