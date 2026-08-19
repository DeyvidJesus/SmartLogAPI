package br.com.smartlog.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import br.com.smartlog.api.dto.RecursoRequests.RotaRequest;
import br.com.smartlog.api.dto.RecursoResponses.RotaResponse;
import br.com.smartlog.application.service.RotaService;
import br.com.smartlog.security.AuthenticatedUser;
import br.com.smartlog.domain.model.Rota;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/rotas")
@Tag(name = "Rotas", description = "Planejamento e consulta de rotas")
public class RotaController {
    private final RotaService service;
    public RotaController(RotaService service) { this.service = service; }
    @GetMapping
    @Operation(summary = "Lista rotas da empresa")
    @PreAuthorize("hasAnyRole('ADMIN', 'MOTORISTA')")
    public List<RotaResponse> listar(Authentication auth) {
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        return service.listar(user.empresaId(), user.isMotorista() ? user.uid() : null).stream().map(RotaResponse::from).toList();
    }
    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma rota")
    @PreAuthorize("hasAnyRole('ADMIN', 'MOTORISTA')")
    public RotaResponse buscar(Authentication auth, @PathVariable String id) {
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        var rota = service.buscar(user.empresaId(), id);
        if (user.isMotorista() && !user.uid().equals(rota.motoristaId())) throw new org.springframework.security.access.AccessDeniedException("Rota fora do escopo");
        return RotaResponse.from(rota);
    }
    @PostMapping
    @Operation(summary = "Cria uma rota", description = "Disponível somente para ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RotaResponse> criar(Authentication auth, @Valid @RequestBody RotaRequest request) {
        return ResponseEntity.status(201).body(RotaResponse.from(save(auth, null, request)));
    }
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma rota")
    @PreAuthorize("hasRole('ADMIN')")
    public RotaResponse atualizar(Authentication auth, @PathVariable String id, @Valid @RequestBody RotaRequest request) { return RotaResponse.from(save(auth, id, request)); }
    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma rota")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(Authentication auth, @PathVariable String id) { service.excluir(user(auth).empresaId(), id); return ResponseEntity.noContent().build(); }
    private Rota save(Authentication a, String id, RotaRequest r) { return service.salvar(user(a).empresaId(), id, r.nome(), r.motoristaId(), r.status()); }
    private AuthenticatedUser user(Authentication a) { return (AuthenticatedUser) a.getPrincipal(); }
}
