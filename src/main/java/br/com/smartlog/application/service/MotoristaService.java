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
    private final AccountProvisioner accounts;
    public MotoristaService(MotoristaRepository repository, AccountProvisioner accounts) { this.repository = repository; this.accounts = accounts; }
    public List<Motorista> listar(String tenant) { return repository.findAll(tenant); }
    public Motorista buscar(String tenant, String id) { return repository.findById(tenant, id).orElseThrow(() -> new ResourceNotFoundException("Motorista", id)); }
    public Motorista salvar(String tenant, String id, String uid, String nome, String email, boolean ativo) {
        Instant now = Instant.now(); Motorista old = id == null ? null : repository.findById(tenant, id).orElse(null);
        var account=accounts.prepare(tenant, old == null ? uid : old.uid(), email, nome, "MOTORISTA");
        Motorista value = new Motorista(id == null ? account.uid() : id, tenant, account.uid(), nome, email.trim().toLowerCase(), ativo,
                old == null ? now : old.criadaEm(), now);
        try { return repository.save(value); } catch(RuntimeException e) { accounts.rollback(account); throw e; }
    }
    public void excluir(String tenant, String id) { buscar(tenant, id); repository.delete(tenant, id); }
}
