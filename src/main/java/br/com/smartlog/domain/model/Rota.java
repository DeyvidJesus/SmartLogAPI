package br.com.smartlog.domain.model;
import java.time.Instant;
public record Rota(String id, String empresaId, String nome, String motoristaId,
        String status, Instant criadaEm, Instant atualizadaEm, Localizacao localizacaoAtual) {
    public Rota(String id, String empresaId, String nome, String motoristaId, String status, Instant criadaEm, Instant atualizadaEm) {
        this(id, empresaId, nome, motoristaId, status, criadaEm, atualizadaEm, null);
    }
    public record Localizacao(double latitude, double longitude, Instant atualizadoEm) { }
}
