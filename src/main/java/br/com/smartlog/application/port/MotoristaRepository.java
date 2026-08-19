package br.com.smartlog.application.port;

import java.util.List;
import java.util.Optional;
import br.com.smartlog.domain.model.Motorista;

public interface MotoristaRepository {
    List<Motorista> findAll(String empresaId);
    Optional<Motorista> findById(String empresaId, String id);
    Motorista save(Motorista motorista);
    void delete(String empresaId, String id);
}
