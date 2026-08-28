package br.com.smartlog.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import br.com.smartlog.application.port.VeiculoRepository;
import br.com.smartlog.domain.exception.ResourceNotFoundException;
import br.com.smartlog.domain.model.Veiculo;

@Service
public class VeiculoService {
    private final VeiculoRepository repository;

    public VeiculoService(VeiculoRepository repository) {
        this.repository = repository;
    }

    public List<Veiculo> listar(String tenant) {
        return repository.findAll(tenant);
    }

    public Veiculo buscar(String tenant, String id) {
        return repository.findById(tenant, id)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo", id));
    }

    public Veiculo salvar(String tenant, String id, String placa, String modelo, String tipo,
            Double capacidadeKg, Double capacidadeM3, Integer ano, Boolean ativo) {
        Instant now = Instant.now();
        Veiculo old = id == null ? null : repository.findById(tenant, id).orElse(null);
        String finalId = id == null ? UUID.randomUUID().toString() : id;
        Veiculo value = new Veiculo(finalId, tenant, placa, modelo, tipo,
                capacidadeKg != null ? capacidadeKg : 3500.0,
                capacidadeM3 != null ? capacidadeM3 : 18.0,
                ano != null ? ano : 2024,
                ativo != null ? ativo : true,
                old == null ? now : old.criadaEm(), now);
        return repository.save(value);
    }

    public void excluir(String tenant, String id) {
        buscar(tenant, id);
        repository.delete(tenant, id);
    }
}
