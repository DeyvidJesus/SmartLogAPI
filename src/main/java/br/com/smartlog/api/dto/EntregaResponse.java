package br.com.smartlog.api.dto;

import java.time.Instant;

import br.com.smartlog.domain.model.Entrega;
import br.com.smartlog.domain.model.EntregaStatus;

public record EntregaResponse(
        String id,
        String codigo,
        String clienteId,
        String rotaId,
        String motoristaId,
        EntregaStatus status,
        String origem,
        String destino,
        Instant criadaEm,
        Instant atualizadaEm) {

    public static EntregaResponse from(Entrega entrega) {
        return new EntregaResponse(entrega.id(), entrega.codigo(), entrega.clienteId(),
                entrega.rotaId(), entrega.motoristaId(), entrega.status(), entrega.origem(),
                entrega.destino(), entrega.criadaEm(), entrega.atualizadaEm());
    }
}
