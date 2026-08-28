package br.com.smartlog.infrastructure.firebase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import br.com.smartlog.application.port.RotaRepository;
import br.com.smartlog.domain.model.Rota;

@Repository
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRotaRepository implements RotaRepository {
    private final Map<String, Rota> store = new ConcurrentHashMap<>();

    public InMemoryRotaRepository() {
        Instant now = Instant.now();
        save(new Rota("rota-001", "empresa-001", "Rota Grande SP e Campinas", "motorista-001", "EM_ANDAMENTO", now, now));
        save(new Rota("rota-002", "empresa-001", "Rota Litoral Paulista", "motorista-002", "PLANEJADA", now, now));
    }

    @Override public List<Rota> findAll(String tenant, String motoristaId) {
        List<Rota> list = new ArrayList<>();
        for (Rota r : store.values()) {
            if (!tenant.equals(r.empresaId())) continue;
            if (motoristaId != null && !motoristaId.equals(r.motoristaId())) continue;
            list.add(r);
        }
        return list;
    }
    @Override public Optional<Rota> findById(String tenant, String id) {
        Rota r = store.get(id);
        return (r != null && tenant.equals(r.empresaId())) ? Optional.of(r) : Optional.empty();
    }
    @Override public Rota save(Rota r) {
        store.put(r.id(), r);
        return r;
    }
    @Override public void delete(String tenant, String id) {
        store.remove(id);
    }
}
