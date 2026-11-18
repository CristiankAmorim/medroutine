package br.ucsal.medroutine.data.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Agenda {
    public Usuario idoso;
    public Instant geradaEm;
    public List<Dose> doses = new ArrayList<>();
}
