package br.com.smartlog.security;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseToken;
@Component
public class FirebaseUserDirectory {
    private final ObjectProvider<Firestore> firestore;
    public FirebaseUserDirectory(ObjectProvider<Firestore> firestore) { this.firestore = firestore; }
    public AuthenticatedUser resolve(FirebaseToken token) {
        Map<String,Object> profile = profile(token.getUid());
        String role = String.valueOf(profile.getOrDefault("tipoUsuario", profile.getOrDefault("tipo", ""))).toUpperCase();
        if (role.equals("ADMINISTRADOR")) role = "ADMIN";
        String tenant = String.valueOf(profile.getOrDefault("empresaId", ""));
        if (!validRole(role) || tenant.isBlank() || Boolean.FALSE.equals(profile.get("ativo")))
            throw new IllegalArgumentException("Cadastro sem acesso");
        return new AuthenticatedUser(token.getUid(), tenant, role);
    }
    public Map<String,Object> profile(String uid) {
        Firestore db = firestore.getIfAvailable();
        if (db == null) throw new IllegalArgumentException("Cadastro indisponível");
        try {
            var doc = db.collection("usuarios").document(uid).get().get();
            if (!doc.exists()) throw new IllegalArgumentException("Cadastro inexistente");
            return doc.getData();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalArgumentException("Cadastro indisponível", e); }
        catch (java.util.concurrent.ExecutionException e) { throw new IllegalArgumentException("Cadastro indisponível", e); }
    }
    private static boolean validRole(String role) { return java.util.Set.of("ADMIN", "MOTORISTA", "CLIENTE").contains(role); }
}
