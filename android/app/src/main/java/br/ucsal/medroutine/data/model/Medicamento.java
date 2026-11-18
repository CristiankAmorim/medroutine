package br.ucsal.medroutine.data.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Medicamento {
    public long id;
    public long idosoId;
    public String nome;
    public String dosagem;
    public String orientacoes;
    public LocalDate dataInicio;
    public LocalDate dataFim;
    public boolean ativo = true;
    public List<Horario> horarios = new ArrayList<>();

    @Override
    public String toString() {
        return nome + " " + dosagem;
    }
}
