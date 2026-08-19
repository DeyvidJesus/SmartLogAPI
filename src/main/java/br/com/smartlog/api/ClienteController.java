package br.com.smartlog.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import br.com.smartlog.api.dto.RecursoRequests.ClienteRequest;
import br.com.smartlog.api.dto.RecursoResponses.ClienteResponse;
import br.com.smartlog.application.service.ClienteService;
import br.com.smartlog.security.AuthenticatedUser;
import br.com.smartlog.domain.model.Cliente;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/clientes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Clientes", description = "Cadastro de clientes corporativos")
public class ClienteController {
    private final ClienteService service;
    public ClienteController(ClienteService service) { this.service = service; }
    @GetMapping @Operation(summary = "Lista clientes da empresa") public List<ClienteResponse> listar(Authentication a) { return service.listar(user(a).empresaId()).stream().map(ClienteResponse::from).toList(); }
    @GetMapping("/{id}") @Operation(summary = "Consulta um cliente") public ClienteResponse buscar(Authentication a, @PathVariable String id) { return ClienteResponse.from(service.buscar(user(a).empresaId(), id)); }
    @PostMapping @Operation(summary = "Cria um cliente") public ResponseEntity<ClienteResponse> criar(Authentication a, @Valid @RequestBody ClienteRequest r) { return ResponseEntity.status(201).body(ClienteResponse.from(save(a, null, r))); }
    @PutMapping("/{id}") @Operation(summary = "Atualiza um cliente") public ClienteResponse atualizar(Authentication a, @PathVariable String id, @Valid @RequestBody ClienteRequest r) { return ClienteResponse.from(save(a, id, r)); }
    @DeleteMapping("/{id}") @Operation(summary = "Remove um cliente") public ResponseEntity<Void> excluir(Authentication a, @PathVariable String id) { service.excluir(user(a).empresaId(), id); return ResponseEntity.noContent().build(); }
    private Cliente save(Authentication a, String id, ClienteRequest r) { return service.salvar(user(a).empresaId(), id, r.nome(), r.email(), r.telefone(), r.ativo()); }
    private AuthenticatedUser user(Authentication a) { return (AuthenticatedUser) a.getPrincipal(); }
}
