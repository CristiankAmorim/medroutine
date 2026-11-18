package br.ucsal.medroutine.data.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicamentoRequest {
    public String nome;
    public String dosagem;
    public String orientacoes;
    public LocalDate dataInicio;
    public LocalDate dataFim;
    public List<Horario> horarios = new ArrayList<>();
}
