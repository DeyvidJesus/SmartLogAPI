package br.com.smartlog.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.smartlog.api.dto.AtualizarStatusRequest;
import br.com.smartlog.api.dto.CriarEntregaRequest;
import br.com.smartlog.api.dto.EntregaListResponse;
import br.com.smartlog.api.dto.EntregaResponse;
import br.com.smartlog.application.service.EntregaService;
import br.com.smartlog.domain.model.EntregaStatus;
import br.com.smartlog.security.AuthenticatedUser;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/entregas")
@Tag(name = "Entregas", description = "Acompanhamento e operação de entregas")
public class EntregaController {

    private final EntregaService service;

    public EntregaController(EntregaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista entregas dentro do escopo do usuário autenticado")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária")})
    public EntregaListResponse listar(Authentication authentication,
            @RequestParam(required = false) EntregaStatus status,
            @RequestParam(required = false) String rotaId,
            @RequestParam(required = false) String motoristaId,
            @RequestParam(required = false) String clienteId) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        String scopedMotorista = user.isMotorista() ? user.uid() : motoristaId;
        String scopedCliente = user.isCliente() ? user.uid() : clienteId;
        List<EntregaResponse> data = service.listar(user.empresaId(), status, rotaId,
                scopedMotorista, scopedCliente).stream().map(EntregaResponse::from).toList();
        return new EntregaListResponse(data);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma entrega")
    public EntregaResponse buscar(Authentication authentication, @PathVariable String id) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        var entrega = service.buscar(user.empresaId(), id);
        ensureResourceScope(user, entrega.clienteId(), entrega.motoristaId());
        return EntregaResponse.from(entrega);
    }

    @PostMapping
    @Operation(summary = "Cria uma entrega", description = "Disponível somente para ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntregaResponse> criar(Authentication authentication,
            @Valid @RequestBody CriarEntregaRequest request) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ResponseEntity.status(201).body(EntregaResponse.from(service.criar(user.empresaId(),
                request.clienteId(), request.rotaId(), request.origem(), request.destino())));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualiza o status de uma entrega")
    @PreAuthorize("hasAnyRole('ADMIN', 'MOTORISTA')")
    public EntregaResponse atualizarStatus(Authentication authentication, @PathVariable String id,
            @Valid @RequestBody AtualizarStatusRequest request) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        var entrega = service.buscar(user.empresaId(), id);
        ensureResourceScope(user, entrega.clienteId(), entrega.motoristaId());
        return EntregaResponse.from(service.atualizarStatus(user.empresaId(), id, request.status()));
    }

    private static void ensureResourceScope(AuthenticatedUser user, String clienteId, String motoristaId) {
        if (user.isAdmin()) return;
        if (user.isCliente() && user.uid().equals(clienteId)) return;
        if (user.isMotorista() && user.uid().equals(motoristaId)) return;
        throw new AccessDeniedException("Recurso fora do escopo do usuário");
    }
}
