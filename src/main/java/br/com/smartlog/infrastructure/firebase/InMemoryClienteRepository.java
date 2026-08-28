package br.com.smartlog.infrastructure.firebase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import br.com.smartlog.application.port.ClienteRepository;
import br.com.smartlog.domain.model.Cliente;

@Repository
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryClienteRepository implements ClienteRepository {
    private final Map<String, Cliente> store = new ConcurrentHashMap<>();

    public InMemoryClienteRepository() {
        Instant now = Instant.now();
        save(new Cliente("cliente-001", "empresa-001", "Logística ABC Ltda", "contato@logisticaabc.com.br", "11987654321", true, now, now));
        save(new Cliente("cliente-002", "empresa-001", "Distribuidora Nacional", "sac@distribuidoranacional.com.br", "11976543210", true, now, now));
        save(new Cliente("cliente-003", "empresa-001", "Varejo Express", "suporte@varejoexpress.com.br", "11965432109", true, now, now));
    }

    @Override public List<Cliente> findAll(String tenant) {
        List<Cliente> list = new ArrayList<>();
        for (Cliente c : store.values()) if (tenant.equals(c.empresaId())) list.add(c);
        return list;
    }
    @Override public Optional<Cliente> findById(String tenant, String id) {
        Cliente c = store.get(id);
        return (c != null && tenant.equals(c.empresaId())) ? Optional.of(c) : Optional.empty();
    }
    @Override public Cliente save(Cliente c) {
        store.put(c.id(), c);
        return c;
    }
    @Override public void delete(String tenant, String id) {
        store.remove(id);
    }
}
