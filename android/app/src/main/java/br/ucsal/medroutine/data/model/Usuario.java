package br.ucsal.medroutine.data.model;

public class Usuario {
    public static final String IDOSO = "IDOSO";
    public static final String CUIDADOR = "CUIDADOR";

    public long id;
    public String nome;
    public String email;
    public String tipo;
    public String fusoHorario;
    public boolean anuncioVoz;
}
