package br.com.smartlog.application.port;

import java.util.List;
import java.util.Optional;
import br.com.smartlog.domain.model.Cliente;

public interface ClienteRepository {
    List<Cliente> findAll(String empresaId);
    Optional<Cliente> findById(String empresaId, String id);
    Cliente save(Cliente cliente);
    void delete(String empresaId, String id);
}
