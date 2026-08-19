package br.com.smartlog.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import br.com.smartlog.application.port.MotoristaRepository;
import br.com.smartlog.domain.exception.ResourceNotFoundException;
import br.com.smartlog.domain.model.Motorista;

@Service
public class MotoristaService {
    private final MotoristaRepository repository;
    public MotoristaService(MotoristaRepository repository) { this.repository = repository; }
    public List<Motorista> listar(String tenant) { return repository.findAll(tenant); }
    public Motorista buscar(String tenant, String id) { return repository.findById(tenant, id).orElseThrow(() -> new ResourceNotFoundException("Motorista", id)); }
    public Motorista salvar(String tenant, String id, String uid, String nome, String email, boolean ativo) {
        Instant now = Instant.now(); Motorista old = id == null ? null : repository.findById(tenant, id).orElse(null);
        Motorista value = new Motorista(id == null ? UUID.randomUUID().toString() : id, tenant, uid, nome, email, ativo,
                old == null ? now : old.criadaEm(), now); return repository.save(value);
    }
    public void excluir(String tenant, String id) { buscar(tenant, id); repository.delete(tenant, id); }
}
