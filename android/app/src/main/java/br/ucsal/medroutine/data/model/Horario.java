package br.ucsal.medroutine.data.model;

import java.util.ArrayList;
import java.util.List;

public class Horario {
    public Long id;
    public String hora;
    public String tipoFrequencia;
    public Integer intervaloHoras;
    public List<String> diasSemana = new ArrayList<>();
    public Integer toleranciaMinutos;
}
