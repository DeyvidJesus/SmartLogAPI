package br.com.smartlog.infrastructure.firebase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import br.com.smartlog.application.port.MotoristaRepository;
import br.com.smartlog.domain.model.Motorista;

@Repository
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryMotoristaRepository implements MotoristaRepository {
    private final Map<String, Motorista> store = new ConcurrentHashMap<>();

    public InMemoryMotoristaRepository() {
        Instant now = Instant.now();
        save(new Motorista("motorista-001", "empresa-001", "uid-motorista-001", "Carlos Silva", "carlos.silva@smartlog.com", true, now, now));
        save(new Motorista("motorista-002", "empresa-001", "uid-motorista-002", "Marcos Oliveira", "marcos.oliveira@smartlog.com", true, now, now));
    }

    @Override public List<Motorista> findAll(String tenant) {
        List<Motorista> list = new ArrayList<>();
        for (Motorista m : store.values()) if (tenant.equals(m.empresaId())) list.add(m);
        return list;
    }
    @Override public Optional<Motorista> findById(String tenant, String id) {
        Motorista m = store.get(id);
        return (m != null && tenant.equals(m.empresaId())) ? Optional.of(m) : Optional.empty();
    }
    @Override public Motorista save(Motorista m) {
        store.put(m.id(), m);
        return m;
    }
    @Override public void delete(String tenant, String id) {
        store.remove(id);
    }
}
