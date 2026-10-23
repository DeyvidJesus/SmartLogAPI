package br.com.smartlog.application.service;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.google.firebase.auth.*;
import com.google.cloud.firestore.Firestore;
/** Admin-invoked account provisioning. Never returns a password to a client. */
@Service
public class AccountProvisioner {
    private final ObjectProvider<FirebaseAuth> auth;
    private final ObjectProvider<Firestore> firestore;
    public record Account(String uid, boolean created) { }
    public AccountProvisioner(ObjectProvider<FirebaseAuth> auth,ObjectProvider<Firestore> firestore) { this.auth=auth;this.firestore=firestore; }
    public Account prepare(String tenant,String requestedUid,String email,String name,String role) {
        FirebaseAuth firebase=auth.getIfAvailable();
        if(firebase==null) return new Account(requestedUid==null||requestedUid.isBlank()?UUID.randomUUID().toString():requestedUid,false);
        try {
            UserRecord user=null;
            boolean explicit=requestedUid!=null&&!requestedUid.isBlank();
            try { user=explicit?firebase.getUser(requestedUid):firebase.getUserByEmail(email.trim().toLowerCase()); }
            catch(FirebaseAuthException e) {
                if(e.getAuthErrorCode()!=AuthErrorCode.USER_NOT_FOUND) throw e;
                if(explicit) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"O UID informado não existe no Firebase.");
            }
            if(user!=null) {
                if(!email.trim().equalsIgnoreCase(user.getEmail())) throw new ResponseStatusException(HttpStatus.CONFLICT,"UID e e-mail não correspondem.");
                var profile=firestore.getObject().collection("usuarios").document(user.getUid()).get().get();
                if(profile.exists()) {
                    String existingRole=String.valueOf(profile.get("tipoUsuario") == null ? profile.get("tipo") : profile.get("tipoUsuario"));
                    if(!tenant.equals(profile.getString("empresaId")) || !role.equalsIgnoreCase(existingRole))
                        throw new ResponseStatusException(HttpStatus.CONFLICT,"Essa conta já possui outro vínculo. Utilize o cadastro existente.");
                }
                return new Account(user.getUid(),false);
            }
            user=firebase.createUser(new UserRecord.CreateRequest().setEmail(email.trim().toLowerCase()).setDisplayName(name)
                .setPassword(UUID.randomUUID().toString()+UUID.randomUUID()).setEmailVerified(false));
            return new Account(user.getUid(),true);
        } catch(ResponseStatusException e) { throw e; }
        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cadastro indisponível."); }
        catch(Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Não foi possível provisionar a conta Firebase."); }
    }
    public void rollback(Account account) {
        if(!account.created())return;
        try { auth.getObject().deleteUser(account.uid()); }
        catch(FirebaseAuthException e) { org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha ao remover conta sem perfil após erro no cadastro"); }
    }
}
