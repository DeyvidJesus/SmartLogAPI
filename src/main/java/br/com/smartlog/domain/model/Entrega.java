package br.com.smartlog.domain.model;
import java.time.Instant;
public record Entrega(String id,String codigo,String empresaId,String clienteId,String rotaId,
        String motoristaId,EntregaStatus status,String origem,String destino,Instant criadaEm,Instant atualizadaEm,Carga carga) {
    public Entrega {
        carga = carga == null ? Carga.empty() : carga;
    }
    public Entrega(String id,String codigo,String empresaId,String clienteId,String rotaId,
            String motoristaId,EntregaStatus status,String origem,String destino,Instant criadaEm,Instant atualizadaEm) {
        this(id,codigo,empresaId,clienteId,rotaId,motoristaId,status,origem,destino,criadaEm,atualizadaEm,Carga.empty());
    }
    public record Carga(String chaveNfe,Double valorNfe,Double pesoKg,Double volumeM3,Integer volumes,String janelaEntrega) {
        public static Carga empty() { return new Carga(null,null,null,null,null,null); }
    }
}
