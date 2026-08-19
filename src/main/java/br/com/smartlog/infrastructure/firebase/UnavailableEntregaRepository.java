package br.com.smartlog.infrastructure.firebase;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.smartlog.application.port.EntregaRepository;
import br.com.smartlog.domain.exception.PersistenceException;
import br.com.smartlog.domain.model.Entrega;
import br.com.smartlog.domain.model.EntregaStatus;

@Repository
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailableEntregaRepository implements EntregaRepository {

    private PersistenceException unavailable() {
        return new PersistenceException("Firestore não está habilitado", null);
    }

    @Override
    public List<Entrega> findAll(String empresaId, EntregaStatus status, String rotaId,
            String motoristaId, String clienteId) {
        throw unavailable();
    }

    @Override
    public Optional<Entrega> findById(String empresaId, String id) {
        throw unavailable();
    }

    @Override
    public Entrega save(Entrega entrega) {
        throw unavailable();
    }
}
