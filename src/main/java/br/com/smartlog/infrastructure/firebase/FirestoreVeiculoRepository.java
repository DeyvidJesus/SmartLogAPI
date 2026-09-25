package br.com.smartlog.infrastructure.firebase;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import com.google.cloud.firestore.Firestore;
import br.com.smartlog.application.port.VeiculoRepository;
import br.com.smartlog.domain.model.Veiculo;

@Repository
@ConditionalOnBean(Firestore.class)
public class FirestoreVeiculoRepository implements VeiculoRepository {
    private final FirestoreStore store;
    public FirestoreVeiculoRepository(FirestoreStore store) { this.store = store; }
    public List<Veiculo> findAll(String tenant) { return store.findAll(tenant, "veiculos", null, null).stream().map(FirestoreResourceMapper::veiculo).toList(); }
    public Optional<Veiculo> findById(String tenant, String id) { return store.find(tenant, "veiculos", id).map(FirestoreResourceMapper::veiculo); }
    public Veiculo save(Veiculo v) { store.save(v.empresaId(), "veiculos", v.id(), FirestoreResourceMapper.veiculoMap(v)); return v; }
    public void delete(String tenant, String id) { store.delete(tenant, "veiculos", id); }
}
