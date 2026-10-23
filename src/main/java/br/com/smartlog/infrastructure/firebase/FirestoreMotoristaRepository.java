package br.com.smartlog.infrastructure.firebase;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import com.google.cloud.firestore.Firestore;
import br.com.smartlog.application.port.MotoristaRepository;
import br.com.smartlog.domain.model.Motorista;

@Repository
@ConditionalOnBean(Firestore.class)
public class FirestoreMotoristaRepository implements MotoristaRepository {
    private final FirestoreStore store;
    public FirestoreMotoristaRepository(FirestoreStore store) { this.store = store; }
    public List<Motorista> findAll(String tenant) { return store.findAll(tenant, "usuarios", null, null).stream().filter(d -> "MOTORISTA".equals(AppSchema.key(AppSchema.text(d, "tipoUsuario", "tipo")))).map(FirestoreResourceMapper::motorista).toList(); }
    public Optional<Motorista> findById(String tenant, String id) { return store.find(tenant, "usuarios", id).map(FirestoreResourceMapper::motorista); }
    public Motorista save(Motorista m) { var data = FirestoreResourceMapper.motoristaMap(m.id(), m.uid(), m.nome(), m.email(), m.ativo(), m.criadaEm(), m.atualizadaEm()); data.put("tipoUsuario", "MOTORISTA"); store.save(m.empresaId(), "usuarios", m.id(), data); return m; }
    public void delete(String tenant, String id) { store.delete(tenant, "usuarios", id); }
}
