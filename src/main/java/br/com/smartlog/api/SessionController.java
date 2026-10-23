package br.com.smartlog.api;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.google.cloud.firestore.Firestore;
import br.com.smartlog.security.*;
@RestController
@RequestMapping("/api/v1/me")
public class SessionController {
    private final FirebaseUserDirectory directory;
    private final ObjectProvider<Firestore> firestore;
    public SessionController(FirebaseUserDirectory directory, ObjectProvider<Firestore> firestore) {
        this.directory = directory; this.firestore = firestore;
    }
    @GetMapping
    public Map<String,Object> me(Authentication auth) {
        AuthenticatedUser u = (AuthenticatedUser) auth.getPrincipal();
        Map<String,Object> profile = firestore.getIfAvailable() == null ? Map.of() : directory.profile(u.uid());
        return Map.of("uid", u.uid(), "empresaId", u.empresaId(), "role", u.role(),
                "nome", profile.getOrDefault("nome", "Operação de demonstração"),
                "email", profile.getOrDefault("email", ""),
                "demo", firestore.getIfAvailable() == null);
    }
}
