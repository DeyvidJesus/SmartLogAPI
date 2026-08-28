package br.com.smartlog.api.dto;

import jakarta.validation.constraints.NotBlank;

public final class RecursoRequests {
    private RecursoRequests() { }

    public record RotaRequest(
            @NotBlank(message = "nome obrigatório") String nome,
            String motoristaId,
            String status) {
    }

    public record MotoristaRequest(
            String uid,
            @NotBlank(message = "nome obrigatório") String nome,
            @NotBlank(message = "email obrigatório") String email,
            Boolean ativo) {
    }

    public record ClienteRequest(
            @NotBlank(message = "nome obrigatório") String nome,
            @NotBlank(message = "email obrigatório") String email,
            String telefone,
            Boolean ativo) {
    }

    public record VeiculoRequest(
            @NotBlank(message = "placa obrigatória") String placa,
            @NotBlank(message = "modelo obrigatório") String modelo,
            String tipo,
            Double capacidadeKg,
            Double capacidadeM3,
            Integer ano,
            Boolean ativo) {
    }
}
