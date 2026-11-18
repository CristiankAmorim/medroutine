package br.ucsal.medroutine.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IdosoRequest(@NotBlank(message = "Informe o nome do idoso.") @Size(max = 120) String nome,
                           String fusoHorario,
                           Boolean anuncioVoz) { }
