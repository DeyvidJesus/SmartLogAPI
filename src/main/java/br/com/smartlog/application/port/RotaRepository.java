package br.com.smartlog.application.port;

import java.util.List;
import java.util.Optional;
import br.com.smartlog.domain.model.Rota;

public interface RotaRepository {
    List<Rota> findAll(String empresaId, String motoristaId);
    Optional<Rota> findById(String empresaId, String id);
    Rota save(Rota rota);
    void delete(String empresaId, String id);
}
