package br.com.smartlog.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class RecursoRequests {
    private RecursoRequests() { }
    public record RotaRequest(@NotBlank String nome, @NotBlank String motoristaId, @NotBlank String status) { }
    public record ClienteRequest(@NotBlank String nome, @NotBlank @Email String email, String telefone, @NotNull Boolean ativo) { }
    public record MotoristaRequest(@NotBlank String uid, @NotBlank String nome, @NotBlank @Email String email, @NotNull Boolean ativo) { }
}
