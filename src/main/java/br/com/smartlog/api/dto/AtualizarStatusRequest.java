package br.com.smartlog.api.dto;

import br.com.smartlog.domain.model.EntregaStatus;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusRequest(@NotNull EntregaStatus status) {
}
