package br.com.smartlog.infrastructure.firebase;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import com.google.cloud.firestore.Firestore;
import br.com.smartlog.application.port.ClienteRepository;
import br.com.smartlog.domain.model.Cliente;

@Repository
@ConditionalOnBean(Firestore.class)
public class FirestoreClienteRepository implements ClienteRepository {
    private final FirestoreStore store;
    public FirestoreClienteRepository(FirestoreStore store) { this.store = store; }
    public List<Cliente> findAll(String tenant) { return store.findAll(tenant, "usuarios", null, null).stream().filter(d -> "CLIENTE".equals(AppSchema.key(AppSchema.text(d, "tipoUsuario", "tipo")))).map(FirestoreResourceMapper::cliente).toList(); }
    public Optional<Cliente> findById(String tenant, String id) { return store.find(tenant, "usuarios", id).map(FirestoreResourceMapper::cliente); }
    public Cliente save(Cliente c) { var data = FirestoreResourceMapper.map(c.id(), c.nome(), c.email(), c.telefone(), c.ativo(), c.criadaEm(), c.atualizadaEm()); data.put("tipoUsuario", "CLIENTE"); store.save(c.empresaId(), "usuarios", c.id(), data); return c; }
    public void delete(String tenant, String id) { store.delete(tenant, "usuarios", id); }
}
