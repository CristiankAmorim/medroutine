package br.ucsal.medroutine.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank(message = "Informe seu nome.") @Size(max = 120) String nome,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
        @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.") String senha,
        boolean consentimento) { }
