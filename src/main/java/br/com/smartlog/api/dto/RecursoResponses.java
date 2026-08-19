package br.com.smartlog.api.dto;

import java.time.Instant;
import br.com.smartlog.domain.model.Cliente;
import br.com.smartlog.domain.model.Motorista;
import br.com.smartlog.domain.model.Rota;

public final class RecursoResponses {
    private RecursoResponses() { }
    public record RotaResponse(String id, String nome, String motoristaId, String status, Instant criadaEm, Instant atualizadaEm) {
        public static RotaResponse from(Rota r) { return new RotaResponse(r.id(), r.nome(), r.motoristaId(), r.status(), r.criadaEm(), r.atualizadaEm()); }
    }
    public record ClienteResponse(String id, String nome, String email, String telefone, boolean ativo, Instant criadaEm, Instant atualizadaEm) {
        public static ClienteResponse from(Cliente c) { return new ClienteResponse(c.id(), c.nome(), c.email(), c.telefone(), c.ativo(), c.criadaEm(), c.atualizadaEm()); }
    }
    public record MotoristaResponse(String id, String uid, String nome, String email, boolean ativo, Instant criadaEm, Instant atualizadaEm) {
        public static MotoristaResponse from(Motorista m) { return new MotoristaResponse(m.id(), m.uid(), m.nome(), m.email(), m.ativo(), m.criadaEm(), m.atualizadaEm()); }
    }
}
