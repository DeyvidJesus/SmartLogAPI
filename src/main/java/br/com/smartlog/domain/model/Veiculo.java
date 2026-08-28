package br.com.smartlog.domain.model;

import java.time.Instant;

public record Veiculo(
        String id,
        String empresaId,
        String placa,
        String modelo,
        String tipo,
        Double capacidadeKg,
        Double capacidadeM3,
        Integer ano,
        boolean ativo,
        Instant criadaEm,
        Instant atualizadaEm) {
}
