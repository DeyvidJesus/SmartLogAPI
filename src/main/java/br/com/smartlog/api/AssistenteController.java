package br.com.smartlog.api;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import br.com.smartlog.application.service.AssistenteService;
import br.com.smartlog.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
@RestController
@RequestMapping("/api/v1")
public class AssistenteController {
    private final AssistenteService service;
    public AssistenteController(AssistenteService service) { this.service = service; }
    public record Pergunta(@NotBlank @Size(max=500) String pergunta, @Size(max=40) java.util.List<Map<String,String>> historico) { }
    @PostMapping({"/chat", "/assistente"})
    @Operation(summary="Consulta a operação do usuário com IA", description="Contexto derivado do usuário autenticado; 503 sem configuração e 429 quando limitado.")
    public Map<String,String> chat(Authentication auth, @Valid @RequestBody Pergunta pergunta) {
        AuthenticatedUser u = (AuthenticatedUser) auth.getPrincipal();
        return Map.of("resposta", service.perguntar(u, pergunta.pergunta().trim(), pergunta.historico()), "perfil", u.role(), "provedor", "gemini");
    }
}
