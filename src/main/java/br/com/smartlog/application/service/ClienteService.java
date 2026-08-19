package br.com.smartlog.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import br.com.smartlog.application.port.ClienteRepository;
import br.com.smartlog.domain.exception.ResourceNotFoundException;
import br.com.smartlog.domain.model.Cliente;

@Service
public class ClienteService {
    private final ClienteRepository repository;
    public ClienteService(ClienteRepository repository) { this.repository = repository; }
    public List<Cliente> listar(String tenant) { return repository.findAll(tenant); }
    public Cliente buscar(String tenant, String id) { return repository.findById(tenant, id).orElseThrow(() -> new ResourceNotFoundException("Cliente", id)); }
    public Cliente salvar(String tenant, String id, String nome, String email, String telefone, boolean ativo) {
        Instant now = Instant.now(); Cliente old = id == null ? null : repository.findById(tenant, id).orElse(null);
        Cliente value = new Cliente(id == null ? UUID.randomUUID().toString() : id, tenant, nome, email, telefone, ativo,
                old == null ? now : old.criadaEm(), now); return repository.save(value);
    }
    public void excluir(String tenant, String id) { buscar(tenant, id); repository.delete(tenant, id); }
}
