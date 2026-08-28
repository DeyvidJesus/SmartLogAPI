package br.com.smartlog.infrastructure.firebase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import br.com.smartlog.application.port.VeiculoRepository;
import br.com.smartlog.domain.model.Veiculo;

@Repository
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryVeiculoRepository implements VeiculoRepository {
    private final Map<String, Veiculo> store = new ConcurrentHashMap<>();

    public InMemoryVeiculoRepository() {
        Instant now = Instant.now();
        save(new Veiculo("veic-001", "empresa-001", "BRA2E19", "Mercedes-Benz Accelo 1016 (VUC)", "VUC", 5200.0, 24.0, 2024, true, now, now));
        save(new Veiculo("veic-002", "empresa-001", "LOG8X44", "Volkswagen Delivery 11.180 (Toco)", "TOCO", 7500.0, 35.0, 2023, true, now, now));
        save(new Veiculo("veic-003", "empresa-001", "SLG9A01", "Iveco Daily 35S15 (Furgão Express)", "VAN", 1800.0, 12.0, 2024, true, now, now));
        save(new Veiculo("veic-004", "empresa-001", "CAR7B22", "Scania P320 (Truck Pesado)", "TRUCK", 14000.0, 58.0, 2022, true, now, now));
    }

    @Override public List<Veiculo> findAll(String tenant) {
        List<Veiculo> list = new ArrayList<>();
        for (Veiculo v : store.values()) if (tenant.equals(v.empresaId())) list.add(v);
        return list;
    }

    @Override public Optional<Veiculo> findById(String tenant, String id) {
        Veiculo v = store.get(id);
        return (v != null && tenant.equals(v.empresaId())) ? Optional.of(v) : Optional.empty();
    }

    @Override public Veiculo save(Veiculo v) {
        store.put(v.id(), v);
        return v;
    }

    @Override public void delete(String tenant, String id) {
        store.remove(id);
    }
}
