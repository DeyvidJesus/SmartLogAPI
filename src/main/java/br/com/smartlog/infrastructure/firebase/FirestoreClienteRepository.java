package br.com.smartlog.infrastructure.firebase;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import br.com.smartlog.application.port.ClienteRepository;
import br.com.smartlog.domain.model.Cliente;

@Repository
public class FirestoreClienteRepository implements ClienteRepository {
    private final FirestoreStore store;
    public FirestoreClienteRepository(FirestoreStore store) { this.store = store; }
    public List<Cliente> findAll(String tenant) { return store.findAll(tenant, "clientes", null, null).stream().map(FirestoreResourceMapper::cliente).toList(); }
    public Optional<Cliente> findById(String tenant, String id) { return store.find(tenant, "clientes", id).map(FirestoreResourceMapper::cliente); }
    public Cliente save(Cliente c) { store.save(c.empresaId(), "clientes", c.id(), FirestoreResourceMapper.map(c.id(), c.nome(), c.email(), c.telefone(), c.ativo(), c.criadaEm(), c.atualizadaEm())); return c; }
    public void delete(String tenant, String id) { store.delete(tenant, "clientes", id); }
}
