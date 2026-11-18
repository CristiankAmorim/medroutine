package br.ucsal.medroutine.data.model;

import java.time.Instant;

public final class Requisicoes {
    private Requisicoes() { }

    public static class Cadastro {
        public String nome;
        public String email;
        public String senha;
        public boolean consentimento;

        public Cadastro(String nome, String email, String senha, boolean consentimento) {
            this.nome = nome;
            this.email = email;
            this.senha = senha;
            this.consentimento = consentimento;
        }
    }

    public static class Login {
        public String email;
        public String senha;

        public Login(String email, String senha) {
            this.email = email;
            this.senha = senha;
        }
    }

    public static class Email {
        public String email;

        public Email(String email) {
            this.email = email;
        }
    }

    public static class RedefinirSenha {
        public String email;
        public String codigo;
        public String novaSenha;

        public RedefinirSenha(String email, String codigo, String novaSenha) {
            this.email = email;
            this.codigo = codigo;
            this.novaSenha = novaSenha;
        }
    }

    public static class CodigoReq {
        public String codigo;

        public CodigoReq(String codigo) {
            this.codigo = codigo;
        }
    }

    public static class Token {
        public String token;

        public Token(String token) {
            this.token = token;
        }
    }

    public static class IdosoReq {
        public String nome;
        public String fusoHorario;
        public Boolean anuncioVoz;

        public IdosoReq(String nome, String fusoHorario, Boolean anuncioVoz) {
            this.nome = nome;
            this.fusoHorario = fusoHorario;
            this.anuncioVoz = anuncioVoz;
        }
    }

    public static class Confirmar {
        public Instant horarioAparelho;

        public Confirmar(Instant horarioAparelho) {
            this.horarioAparelho = horarioAparelho;
        }
    }
}
