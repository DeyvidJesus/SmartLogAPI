package br.com.smartlog.domain.model;

import java.time.Instant;

public record Rota(String id, String empresaId, String nome, String motoristaId,
        String status, Instant criadaEm, Instant atualizadaEm) { }
