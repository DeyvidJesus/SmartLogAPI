package br.com.smartlog.infrastructure.firebase;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import com.google.cloud.firestore.*;
import br.com.smartlog.domain.exception.PersistenceException;
import org.springframework.security.access.AccessDeniedException;
/** Same root collections as Flutter and the assistant. Tenant is server supplied. */
@Component
public class FirestoreStore {
    private final ObjectProvider<Firestore> firestore;
    public FirestoreStore(ObjectProvider<Firestore> firestore) { this.firestore = firestore; }
    public List<? extends DocumentSnapshot> findAll(String tenant, String name, String field, String value) {
        try {
            Query query = collection(name).whereEqualTo("empresaId", tenant);
            return query.get().get().getDocuments().stream()
                    .filter(d -> field == null || value == null || value.equals(d.getString(field))).toList();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw failure(e); }
        catch (ExecutionException e) { throw failure(e); }
    }
    public Optional<DocumentSnapshot> find(String tenant, String name, String id) {
        try {
            DocumentSnapshot d = collection(name).document(id).get().get();
            return AppSchema.belongs(d, tenant) ? Optional.of(d) : Optional.empty();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw failure(e); }
        catch (ExecutionException e) { throw failure(e); }
    }
    public void save(String tenant, String name, String id, Map<String,Object> data) {
        try {
            Map<String,Object> scoped = new HashMap<>(data);
            scoped.put("empresaId", tenant);
            firestore.getObject().runTransaction(tx -> {
                DocumentReference ref = collection(name).document(id);
                DocumentSnapshot old = tx.get(ref).get();
                if (old.exists() && !AppSchema.belongs(old, tenant)) throw new AccessDeniedException("Tenant inválido");
                tx.set(ref, scoped, SetOptions.merge());
                return null;
            }).get();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw failure(e); }
        catch (ExecutionException e) { throw failure(e); }
    }
    public void delete(String tenant, String name, String id) {
        try {
            firestore.getObject().runTransaction(tx -> {
                DocumentReference ref = collection(name).document(id);
                if (!AppSchema.belongs(tx.get(ref).get(), tenant)) throw new AccessDeniedException("Tenant inválido");
                tx.delete(ref); return null;
            }).get();
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw failure(e); }
        catch (ExecutionException e) { throw failure(e); }
    }
    private CollectionReference collection(String name) {
        Firestore db = firestore.getIfAvailable();
        if (db == null) throw new PersistenceException("Firestore não habilitado", null);
        return db.collection(name);
    }
    private static PersistenceException failure(Exception e) { return new PersistenceException("Falha no Firestore", e); }
}
