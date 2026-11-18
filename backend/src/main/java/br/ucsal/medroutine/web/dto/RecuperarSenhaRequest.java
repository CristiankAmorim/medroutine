package br.ucsal.medroutine.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RecuperarSenhaRequest(@NotBlank(message = "Informe o e-mail.") String email) { }
