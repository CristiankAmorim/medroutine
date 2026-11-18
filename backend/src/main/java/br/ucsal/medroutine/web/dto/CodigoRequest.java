package br.ucsal.medroutine.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CodigoRequest(@NotBlank(message = "Informe o código.") String codigo) { }
