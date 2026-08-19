package br.com.smartlog.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import br.com.smartlog.application.port.RotaRepository;
import br.com.smartlog.domain.exception.ResourceNotFoundException;
import br.com.smartlog.domain.model.Rota;

@Service
public class RotaService {
    private final RotaRepository repository;
    public RotaService(RotaRepository repository) { this.repository = repository; }
    public List<Rota> listar(String tenant, String motoristaId) { return repository.findAll(tenant, motoristaId); }
    public Rota buscar(String tenant, String id) { return repository.findById(tenant, id).orElseThrow(() -> new ResourceNotFoundException("Rota", id)); }
    public Rota salvar(String tenant, String id, String nome, String motoristaId, String status) {
        Instant now = Instant.now(); Rota old = id == null ? null : repository.findById(tenant, id).orElse(null);
        Rota value = new Rota(id == null ? UUID.randomUUID().toString() : id, tenant, nome, motoristaId, status,
                old == null ? now : old.criadaEm(), now); return repository.save(value);
    }
    public void excluir(String tenant, String id) { buscar(tenant, id); repository.delete(tenant, id); }
}
