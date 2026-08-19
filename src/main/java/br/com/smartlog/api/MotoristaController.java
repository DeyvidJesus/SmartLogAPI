package br.com.smartlog.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import br.com.smartlog.api.dto.RecursoRequests.MotoristaRequest;
import br.com.smartlog.api.dto.RecursoResponses.MotoristaResponse;
import br.com.smartlog.application.service.MotoristaService;
import br.com.smartlog.security.AuthenticatedUser;
import br.com.smartlog.domain.model.Motorista;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/motoristas")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Motoristas", description = "Cadastro de motoristas")
public class MotoristaController {
    private final MotoristaService service;
    public MotoristaController(MotoristaService service) { this.service = service; }
    @GetMapping @Operation(summary = "Lista motoristas da empresa") public List<MotoristaResponse> listar(Authentication a) { return service.listar(user(a).empresaId()).stream().map(MotoristaResponse::from).toList(); }
    @GetMapping("/{id}") @Operation(summary = "Consulta um motorista") public MotoristaResponse buscar(Authentication a, @PathVariable String id) { return MotoristaResponse.from(service.buscar(user(a).empresaId(), id)); }
    @PostMapping @Operation(summary = "Cria um motorista") public ResponseEntity<MotoristaResponse> criar(Authentication a, @Valid @RequestBody MotoristaRequest r) { return ResponseEntity.status(201).body(MotoristaResponse.from(save(a, null, r))); }
    @PutMapping("/{id}") @Operation(summary = "Atualiza um motorista") public MotoristaResponse atualizar(Authentication a, @PathVariable String id, @Valid @RequestBody MotoristaRequest r) { return MotoristaResponse.from(save(a, id, r)); }
    @DeleteMapping("/{id}") @Operation(summary = "Remove um motorista") public ResponseEntity<Void> excluir(Authentication a, @PathVariable String id) { service.excluir(user(a).empresaId(), id); return ResponseEntity.noContent().build(); }
    private Motorista save(Authentication a, String id, MotoristaRequest r) { return service.salvar(user(a).empresaId(), id, r.uid(), r.nome(), r.email(), r.ativo()); }
    private AuthenticatedUser user(Authentication a) { return (AuthenticatedUser) a.getPrincipal(); }
}
