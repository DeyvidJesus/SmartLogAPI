package br.com.smartlog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarEntregaRequest(
        @NotBlank String clienteId,
        @NotBlank String rotaId,
        @NotBlank String origem,
        @NotBlank String destino) {
}
