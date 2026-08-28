package br.com.smartlog.application.port;

import java.util.List;
import java.util.Optional;
import br.com.smartlog.domain.model.Veiculo;

public interface VeiculoRepository {
    List<Veiculo> findAll(String tenant);
    Optional<Veiculo> findById(String tenant, String id);
    Veiculo save(Veiculo v);
    void delete(String tenant, String id);
}
