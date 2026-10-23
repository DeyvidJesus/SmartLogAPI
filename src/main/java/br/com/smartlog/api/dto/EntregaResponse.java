package br.com.smartlog.api.dto;
import java.time.Instant;
import br.com.smartlog.domain.model.Entrega;
import br.com.smartlog.domain.model.EntregaStatus;
public record EntregaResponse(String id,String codigo,String empresaId,String clienteId,String rotaId,
        String motoristaId,EntregaStatus status,String origem,String destino,Instant criadaEm,Instant atualizadaEm,
        String chaveNfe,Double valorNfe,Double pesoKg,Double volumeM3,Integer volumes,String janelaEntrega) {
    public static EntregaResponse from(Entrega e) {
        var c=e.carga();
        return new EntregaResponse(e.id(),e.codigo(),e.empresaId(),e.clienteId(),e.rotaId(),e.motoristaId(),e.status(),
            e.origem(),e.destino(),e.criadaEm(),e.atualizadaEm(),c.chaveNfe(),c.valorNfe(),c.pesoKg(),c.volumeM3(),c.volumes(),c.janelaEntrega());
    }
}
