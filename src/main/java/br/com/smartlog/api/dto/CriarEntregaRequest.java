package br.com.smartlog.api.dto;
import jakarta.validation.constraints.*;
import br.com.smartlog.domain.model.Entrega;
public record CriarEntregaRequest(@NotBlank String clienteId,@NotBlank String rotaId,
        @NotBlank @Size(max=500) String origem,@NotBlank @Size(max=500) String destino,
        @Size(max=64) String chaveNfe,@PositiveOrZero Double valorNfe,@PositiveOrZero Double pesoKg,
        @PositiveOrZero Double volumeM3,@PositiveOrZero Integer volumes,
        @Pattern(regexp="MANHA|TARDE|NOITE|COMERCIAL") String janelaEntrega) {
    public Entrega.Carga carga() { return new Entrega.Carga(chaveNfe,valorNfe,pesoKg,volumeM3,volumes,janelaEntrega); }
}
