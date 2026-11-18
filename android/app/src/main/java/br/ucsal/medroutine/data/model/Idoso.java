package br.ucsal.medroutine.data.model;

public class Idoso {
    public long id;
    public String nome;
    public String fusoHorario;
    public boolean anuncioVoz;
    public long totalCuidadores;

    @Override
    public String toString() {
        return nome;
    }
}
