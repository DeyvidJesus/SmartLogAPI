package br.com.smartlog.infrastructure.firebase;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;

import br.com.smartlog.domain.exception.PersistenceException;

@Component
public class FirestoreStore {
    private final ObjectProvider<Firestore> firestore;

    public FirestoreStore(ObjectProvider<Firestore> firestore) { this.firestore = firestore; }

    public List<? extends DocumentSnapshot> findAll(String empresaId, String collection, String field, String value) {
        try {
            Query query = collection(empresaId, collection);
            if (field != null && value != null) query = query.whereEqualTo(field, value);
            return query.get().get().getDocuments();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure("Leitura interrompida", e);
        } catch (ExecutionException e) { throw failure("Falha na leitura", e.getCause()); }
    }

    public Optional<DocumentSnapshot> find(String empresaId, String collection, String id) {
        try {
            DocumentSnapshot snapshot = collection(empresaId, collection).document(id).get().get();
            return snapshot.exists() ? Optional.of(snapshot) : Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure("Leitura interrompida", e);
        } catch (ExecutionException e) { throw failure("Falha na leitura", e.getCause()); }
    }

    public void save(String empresaId, String collection, String id, Map<String, Object> data) {
        try { collection(empresaId, collection).document(id).set(data).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure("Escrita interrompida", e);
        } catch (ExecutionException e) { throw failure("Falha na escrita", e.getCause()); }
    }

    public void delete(String empresaId, String collection, String id) {
        try { collection(empresaId, collection).document(id).delete().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure("Exclusão interrompida", e);
        } catch (ExecutionException e) { throw failure("Falha na exclusão", e.getCause()); }
    }

    private com.google.cloud.firestore.CollectionReference collection(String empresaId, String name) {
        Firestore db = firestore.getIfAvailable();
        if (db == null) throw new PersistenceException("Firestore não está habilitado", null);
        return db.collection("empresas").document(empresaId).collection(name);
    }

    private static PersistenceException failure(String message, Throwable cause) {
        return new PersistenceException(message, cause);
    }
}
