package br.com.smartlog.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import br.com.smartlog.api.dto.RecursoRequests.VeiculoRequest;
import br.com.smartlog.api.dto.RecursoResponses.VeiculoResponse;
import br.com.smartlog.application.service.VeiculoService;
import br.com.smartlog.security.AuthenticatedUser;
import br.com.smartlog.domain.model.Veiculo;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/veiculos")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Veículos", description = "Gestão da frota de veículos e caminhões")
public class VeiculoController {
    private final VeiculoService service;

    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista veículos da frota")
    public List<VeiculoResponse> listar(Authentication auth) {
        return service.listar(user(auth).empresaId()).stream().map(VeiculoResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um veículo")
    public VeiculoResponse buscar(Authentication auth, @PathVariable String id) {
        return VeiculoResponse.from(service.buscar(user(auth).empresaId(), id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um veículo")
    public ResponseEntity<VeiculoResponse> criar(Authentication auth, @Valid @RequestBody VeiculoRequest r) {
        return ResponseEntity.status(201).body(VeiculoResponse.from(save(auth, null, r)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um veículo")
    public VeiculoResponse atualizar(Authentication auth, @PathVariable String id, @Valid @RequestBody VeiculoRequest r) {
        return VeiculoResponse.from(save(auth, id, r));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um veículo")
    public ResponseEntity<Void> excluir(Authentication auth, @PathVariable String id) {
        service.excluir(user(auth).empresaId(), id);
        return ResponseEntity.noContent().build();
    }

    private Veiculo save(Authentication a, String id, VeiculoRequest r) {
        return service.salvar(user(a).empresaId(), id, r.placa(), r.modelo(), r.tipo(),
                r.capacidadeKg(), r.capacidadeM3(), r.ano(), r.ativo());
    }

    private AuthenticatedUser user(Authentication a) {
        return (AuthenticatedUser) a.getPrincipal();
    }
}
