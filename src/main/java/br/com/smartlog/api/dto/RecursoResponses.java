package br.com.smartlog.api.dto;

import java.time.Instant;
import br.com.smartlog.domain.model.Cliente;
import br.com.smartlog.domain.model.Motorista;
import br.com.smartlog.domain.model.Rota;
import br.com.smartlog.domain.model.Veiculo;

public final class RecursoResponses {
    private RecursoResponses() { }

    public record RotaResponse(String id, String empresaId, String nome, String motoristaId,
            String status, Instant criadaEm, Instant atualizadaEm, Rota.Localizacao localizacaoAtual) {
        public static RotaResponse from(Rota r) {
            return new RotaResponse(r.id(), r.empresaId(), r.nome(), r.motoristaId(), r.status(),
                    r.criadaEm(), r.atualizadaEm(), r.localizacaoAtual());
        }
    }

    public record MotoristaResponse(String id, String empresaId, String uid, String nome,
            String email, boolean ativo, Instant criadaEm, Instant atualizadaEm) {
        public static MotoristaResponse from(Motorista m) {
            return new MotoristaResponse(m.id(), m.empresaId(), m.uid(), m.nome(), m.email(),
                    m.ativo(), m.criadaEm(), m.atualizadaEm());
        }
    }

    public record ClienteResponse(String id, String empresaId, String nome, String email,
            String telefone, boolean ativo, Instant criadaEm, Instant atualizadaEm) {
        public static ClienteResponse from(Cliente c) {
            return new ClienteResponse(c.id(), c.empresaId(), c.nome(), c.email(), c.telefone(),
                    c.ativo(), c.criadaEm(), c.atualizadaEm());
        }
    }

    public record VeiculoResponse(String id, String empresaId, String placa, String modelo,
            String tipo, Double capacidadeKg, Double capacidadeM3, Integer ano, boolean ativo,
            Instant criadaEm, Instant atualizadaEm) {
        public static VeiculoResponse from(Veiculo v) {
            return new VeiculoResponse(v.id(), v.empresaId(), v.placa(), v.modelo(), v.tipo(),
                    v.capacidadeKg(), v.capacidadeM3(), v.ano(), v.ativo(), v.criadaEm(), v.atualizadaEm());
        }
    }
}
