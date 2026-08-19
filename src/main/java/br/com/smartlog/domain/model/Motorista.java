package br.com.smartlog.domain.model;

import java.time.Instant;

public record Motorista(String id, String empresaId, String uid, String nome,
        String email, boolean ativo, Instant criadaEm, Instant atualizadaEm) { }
