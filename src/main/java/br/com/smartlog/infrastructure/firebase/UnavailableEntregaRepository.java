package br.com.smartlog.infrastructure.firebase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.smartlog.application.port.EntregaRepository;
import br.com.smartlog.domain.model.Entrega;
import br.com.smartlog.domain.model.EntregaStatus;

@Repository
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailableEntregaRepository implements EntregaRepository {

    private final Map<String, Entrega> store = new ConcurrentHashMap<>();

    public UnavailableEntregaRepository() {
        // Seed com dados iniciais reais para a empresa-001
        Instant now = Instant.now();
        Instant ontem = now.minusSeconds(86400);
        Entrega e1 = new Entrega("ent-001", "ENT-202601", "empresa-001", "cliente-001", "rota-001", "motorista-001",
                EntregaStatus.PENDENTE, "CD Central São Paulo - SP", "Av. Paulista, 1000 - São Paulo, SP", ontem, now);
        Entrega e2 = new Entrega("ent-002", "ENT-202602", "empresa-001", "cliente-002", "rota-001", "motorista-001",
                EntregaStatus.EM_ROTA, "CD Central São Paulo - SP", "Rua das Flores, 250 - Campinas, SP", ontem, now);
        Entrega e3 = new Entrega("ent-003", "ENT-202603", "empresa-001", "cliente-003", "rota-002", null,
                EntregaStatus.ENTREGUE, "Filial Santos - SP", "Av. Brasil, 500 - Santos, SP", ontem.minusSeconds(86400), ontem);
        store.put(e1.id(), e1);
        store.put(e2.id(), e2);
        store.put(e3.id(), e3);
    }

    @Override
    public List<Entrega> findAll(String empresaId, EntregaStatus status, String rotaId,
            String motoristaId, String clienteId) {
        List<Entrega> list = new ArrayList<>();
        for (Entrega e : store.values()) {
            if (!empresaId.equals(e.empresaId())) continue;
            if (status != null && e.status() != status) continue;
            if (rotaId != null && !rotaId.equals(e.rotaId())) continue;
            if (motoristaId != null && !motoristaId.equals(e.motoristaId())) continue;
            if (clienteId != null && !clienteId.equals(e.clienteId())) continue;
            list.add(e);
        }
        return list;
    }

    @Override
    public Optional<Entrega> findById(String empresaId, String id) {
        Entrega e = store.get(id);
        if (e != null && empresaId.equals(e.empresaId())) {
            return Optional.of(e);
        }
        return Optional.empty();
    }

    @Override
    public Entrega save(Entrega entrega) {
        store.put(entrega.id(), entrega);
        return entrega;
    }
}
