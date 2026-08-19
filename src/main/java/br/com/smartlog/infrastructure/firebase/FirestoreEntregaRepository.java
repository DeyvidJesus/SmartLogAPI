package br.com.smartlog.infrastructure.firebase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;

import br.com.smartlog.application.port.EntregaRepository;
import br.com.smartlog.domain.exception.PersistenceException;
import br.com.smartlog.domain.model.Entrega;
import br.com.smartlog.domain.model.EntregaStatus;

@Repository
@ConditionalOnBean(Firestore.class)
public class FirestoreEntregaRepository implements EntregaRepository {

    private final Firestore firestore;

    public FirestoreEntregaRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public List<Entrega> findAll(String empresaId, EntregaStatus status, String rotaId,
            String motoristaId, String clienteId) {
        try {
            Query query = collection(empresaId);
            if (status != null) query = query.whereEqualTo("status", status.name());
            if (rotaId != null) query = query.whereEqualTo("rotaId", rotaId);
            if (motoristaId != null) query = query.whereEqualTo("motoristaId", motoristaId);
            if (clienteId != null) query = query.whereEqualTo("clienteId", clienteId);
            QuerySnapshot snapshot = query.get().get();
            List<Entrega> result = new ArrayList<>();
            for (DocumentSnapshot document : snapshot.getDocuments()) result.add(toDomain(document));
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PersistenceException("Leitura de entregas interrompida", exception);
        } catch (ExecutionException exception) {
            throw new PersistenceException("Falha ao ler entregas no Firestore", exception.getCause());
        }
    }

    @Override
    public Optional<Entrega> findById(String empresaId, String id) {
        try {
            DocumentSnapshot document = collection(empresaId).document(id).get().get();
            return document.exists() ? Optional.of(toDomain(document)) : Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PersistenceException("Leitura da entrega interrompida", exception);
        } catch (ExecutionException exception) {
            throw new PersistenceException("Falha ao ler entrega no Firestore", exception.getCause());
        }
    }

    @Override
    public Entrega save(Entrega entrega) {
        try {
            collection(entrega.empresaId()).document(entrega.id()).set(toDocument(entrega)).get();
            return entrega;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PersistenceException("Escrita da entrega interrompida", exception);
        } catch (ExecutionException exception) {
            throw new PersistenceException("Falha ao salvar entrega no Firestore", exception.getCause());
        }
    }

    private CollectionReference collection(String empresaId) {
        return firestore.collection("empresas").document(empresaId).collection("entregas");
    }

    private static Map<String, Object> toDocument(Entrega entrega) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", entrega.id());
        data.put("codigo", entrega.codigo());
        data.put("clienteId", entrega.clienteId());
        data.put("rotaId", entrega.rotaId());
        data.put("motoristaId", entrega.motoristaId());
        data.put("status", entrega.status().name());
        data.put("origem", entrega.origem());
        data.put("destino", entrega.destino());
        data.put("criadaEm", entrega.criadaEm().toString());
        data.put("atualizadaEm", entrega.atualizadaEm().toString());
        return data;
    }

    private static Entrega toDomain(DocumentSnapshot document) {
        return new Entrega(document.getId(), document.getString("codigo"),
                document.getReference().getParent().getParent().getId(),
                document.getString("clienteId"), document.getString("rotaId"),
                document.getString("motoristaId"), EntregaStatus.valueOf(document.getString("status")),
                document.getString("origem"), document.getString("destino"),
                Instant.parse(document.getString("criadaEm")),
                Instant.parse(document.getString("atualizadaEm")));
    }
}
