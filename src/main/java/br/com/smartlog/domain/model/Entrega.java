package br.com.smartlog.domain.model;

import java.time.Instant;

public record Entrega(
        String id,
        String codigo,
        String empresaId,
        String clienteId,
        String rotaId,
        String motoristaId,
        EntregaStatus status,
        String origem,
        String destino,
        Instant criadaEm,
        Instant atualizadaEm) {
}
