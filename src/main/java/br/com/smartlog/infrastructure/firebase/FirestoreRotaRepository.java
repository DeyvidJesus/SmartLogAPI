package br.com.smartlog.infrastructure.firebase;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import com.google.cloud.firestore.Firestore;
import br.com.smartlog.application.port.RotaRepository;
import br.com.smartlog.domain.model.Rota;

@Repository
@ConditionalOnBean(Firestore.class)
public class FirestoreRotaRepository implements RotaRepository {
    private final FirestoreStore store;
    public FirestoreRotaRepository(FirestoreStore store) { this.store = store; }
    public List<Rota> findAll(String tenant, String motoristaId) {
        return store.findAll(tenant, "rotas", "motoristaId", motoristaId).stream().map(FirestoreResourceMapper::rota).toList();
    }
    public Optional<Rota> findById(String tenant, String id) { return store.find(tenant, "rotas", id).map(FirestoreResourceMapper::rota); }
    public Rota save(Rota r) { store.save(r.empresaId(), "rotas", r.id(), FirestoreResourceMapper.map(r.id(), r.nome(), r.motoristaId(), r.status(), r.criadaEm(), r.atualizadaEm())); return r; }
    public void delete(String tenant, String id) { store.delete(tenant, "rotas", id); }
}
