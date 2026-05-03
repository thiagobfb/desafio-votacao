package br.com.desafio.votacao.voto.api.dto;

import br.com.desafio.votacao.voto.domain.Escolha;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarVotoRequest(
        @NotBlank @Size(max = 64) String associadoId,
        @NotNull Escolha voto
) {
}
