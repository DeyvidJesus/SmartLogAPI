package br.com.smartlog.application.port;

import java.util.List;
import java.util.Optional;

import br.com.smartlog.domain.model.Entrega;
import br.com.smartlog.domain.model.EntregaStatus;

public interface EntregaRepository {

    List<Entrega> findAll(String empresaId, EntregaStatus status, String rotaId,
            String motoristaId, String clienteId);

    Optional<Entrega> findById(String empresaId, String id);

    Entrega save(Entrega entrega);
}
