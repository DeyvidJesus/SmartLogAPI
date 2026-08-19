package br.com.smartlog.domain.model;

import java.time.Instant;

public record Cliente(String id, String empresaId, String nome, String email,
        String telefone, boolean ativo, Instant criadaEm, Instant atualizadaEm) { }
