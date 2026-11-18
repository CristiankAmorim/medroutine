package br.ucsal.medroutine.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(@NotBlank String email,
                                    @NotBlank(message = "Informe o código recebido.") String codigo,
                                    @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
                                    String novaSenha) { }
