package br.com.smartlog.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.smartlog.application.port.EntregaRepository;
import br.com.smartlog.domain.exception.ResourceNotFoundException;
import br.com.smartlog.domain.model.Entrega;
import br.com.smartlog.domain.model.EntregaStatus;

@Service
public class EntregaService {

    private final EntregaRepository repository;

    public EntregaService(EntregaRepository repository) {
        this.repository = repository;
    }

    public List<Entrega> listar(String empresaId, EntregaStatus status, String rotaId,
            String motoristaId, String clienteId) {
        return repository.findAll(empresaId, status, rotaId, motoristaId, clienteId);
    }

    public Entrega buscar(String empresaId, String id) {
        return repository.findById(empresaId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega", id));
    }

    public Entrega criar(String empresaId, String clienteId, String rotaId,
            String origem, String destino) {
        Instant agora = Instant.now();
        Entrega entrega = new Entrega(
                UUID.randomUUID().toString(),
                "ENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                empresaId, clienteId, rotaId, null, EntregaStatus.PENDENTE,
                origem, destino, agora, agora);
        return repository.save(entrega);
    }

    public Entrega atualizarStatus(String empresaId, String id, EntregaStatus status) {
        Entrega atual = buscar(empresaId, id);
        Entrega atualizada = new Entrega(atual.id(), atual.codigo(), atual.empresaId(),
                atual.clienteId(), atual.rotaId(), atual.motoristaId(), status,
                atual.origem(), atual.destino(), atual.criadaEm(), Instant.now());
        return repository.save(atualizada);
    }
}
