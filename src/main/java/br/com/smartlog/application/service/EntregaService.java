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
    private final br.com.smartlog.application.port.RotaRepository rotas;
    private final br.com.smartlog.application.port.ClienteRepository clientes;

    public EntregaService(EntregaRepository repository, br.com.smartlog.application.port.RotaRepository rotas, br.com.smartlog.application.port.ClienteRepository clientes) {
        this.repository = repository;
        this.rotas = rotas;
        this.clientes = clientes;
    }

    public List<Entrega> listar(String empresaId, EntregaStatus status, String rotaId,
            String motoristaId, String clienteId) {
        return repository.findAll(empresaId, status, rotaId, motoristaId, clienteId);
    }

    public boolean pertenceAoCliente(String tenant, String id, String uid) { return repository.pertenceAoCliente(tenant, id, uid); }
    public Entrega buscar(String empresaId, String id) {
        return repository.findById(empresaId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega", id));
    }

    public Entrega criar(String empresaId, String clienteId, String rotaId,
            String origem, String destino) {
        return criar(empresaId, clienteId, rotaId, origem, destino, Entrega.Carga.empty());
    }
    public Entrega criar(String empresaId, String clienteId, String rotaId, String origem, String destino, Entrega.Carga carga) {
        var rota = rotas.findById(empresaId, rotaId).orElseThrow(() -> new ResourceNotFoundException("Rota", rotaId));
        clientes.findById(empresaId, clienteId).orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));
        Instant agora = Instant.now();
        Entrega entrega = new Entrega(
                UUID.randomUUID().toString(),
                "ENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                empresaId, clienteId, rotaId, rota.motoristaId(), EntregaStatus.PENDENTE,
                origem, destino, agora, agora, carga);
        return repository.save(entrega);
    }

    public Entrega atualizarStatus(String empresaId, String id, EntregaStatus status) {
        Entrega atual = buscar(empresaId, id);
        Entrega atualizada = new Entrega(atual.id(), atual.codigo(), atual.empresaId(),
                atual.clienteId(), atual.rotaId(), atual.motoristaId(), status,
                atual.origem(), atual.destino(), atual.criadaEm(), Instant.now(), atual.carga());
        return repository.save(atualizada);
    }
}
