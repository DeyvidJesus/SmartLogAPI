package br.com.smartlog.infrastructure.firebase;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import com.google.cloud.firestore.*;
import br.com.smartlog.application.port.EntregaRepository;
import br.com.smartlog.domain.model.*;
@Repository
@ConditionalOnBean(Firestore.class)
public class FirestoreEntregaRepository implements EntregaRepository {
    private final FirestoreStore store;
    public FirestoreEntregaRepository(FirestoreStore store) { this.store = store; }
    public List<Entrega> findAll(String tenant, EntregaStatus status, String rota, String motorista, String cliente) {
        return store.findAll(tenant, "entregas", null, null).stream()
                .filter(d -> cliente == null || matchesClient(d, tenant, cliente)).map(FirestoreEntregaRepository::toDomain)
                .filter(e -> status == null || e.status() == status)
                .filter(e -> rota == null || rota.equals(e.rotaId()))
                .filter(e -> motorista == null || motorista.equals(e.motoristaId()))
                .toList();
    }
    public Optional<Entrega> findById(String tenant, String id) {
        return store.find(tenant, "entregas", id).map(FirestoreEntregaRepository::toDomain);
    }
    private boolean matchesClient(DocumentSnapshot d, String tenant, String uid) {
        if (uid.equals(AppSchema.text(d, "clienteId"))) return true;
        String email = store.find(tenant, "usuarios", uid).map(u -> AppSchema.text(u, "email")).orElse("");
        return !email.isBlank() && email.equalsIgnoreCase(AppSchema.text(d, "emailCliente"));
    }
    @Override public boolean pertenceAoCliente(String tenant, String id, String uid) {
        return store.find(tenant, "entregas", id).map(d -> matchesClient(d, tenant, uid)).orElse(false);
    }
    public Entrega save(Entrega e) {
        Map<String,Object> data = new HashMap<>();
        data.put("id", e.codigo()); data.put("codigo", e.codigo());
        data.put("clienteId", e.clienteId()); data.put("rotaId", e.rotaId());
        data.put("motoristaId", e.motoristaId()); data.put("status", AppSchema.appStatus(e.status()));
        data.put("origem", e.origem()); data.put("destino", e.destino()); data.put("endereco", e.destino());
        data.put("criadaEm", e.criadaEm().toString()); data.put("atualizadaEm", e.atualizadaEm().toString());
        if (e.status() == EntregaStatus.ENTREGUE) data.put("entregueEm", e.atualizadaEm().toString());
        store.find(e.empresaId(), "usuarios", e.clienteId()).ifPresent(c -> {
            data.put("cliente", AppSchema.text(c, "nome"));
            data.put("emailCliente", AppSchema.text(c, "email").toLowerCase());
        });
        if (e.motoristaId() != null && !e.motoristaId().isBlank()) store.find(e.empresaId(), "usuarios", e.motoristaId()).ifPresent(m -> data.put("motorista", AppSchema.text(m, "nome")));
        store.find(e.empresaId(), "rotas", e.rotaId()).ifPresent(r -> data.put("regiao", AppSchema.text(r, "regiao")));
        var carga=e.carga();
        data.put("chaveNfe", carga.chaveNfe()); data.put("valorNfe", carga.valorNfe());
        data.put("pesoKg", carga.pesoKg()); data.put("volumeM3", carga.volumeM3());
        data.put("volumes", carga.volumes()); data.put("janelaEntrega", carga.janelaEntrega());
        store.save(e.empresaId(), "entregas", e.id(), data);
        return e;
    }
    private static Double number(DocumentSnapshot d,String key) { return d.get(key) instanceof Number n ? n.doubleValue() : null; }
    private static String nullableText(DocumentSnapshot d,String key) { String s=AppSchema.text(d,key); return s.isBlank() ? null : s; }
    static Entrega toDomain(DocumentSnapshot d) {
        return new Entrega(d.getId(), AppSchema.code(d), AppSchema.text(d, "empresaId"),
                AppSchema.text(d, "clienteId"), AppSchema.text(d, "rotaId"),
                AppSchema.text(d, "motoristaId"), AppSchema.status(AppSchema.text(d, "status")),
                AppSchema.text(d, "origem"), AppSchema.text(d, "endereco", "destino"),
                AppSchema.instant(d, "criadaEm", "criadoEm", "dataCriacao"),
                AppSchema.instant(d, "atualizadaEm", "entregueEm", "criadoEm", "criadaEm"),
                new Entrega.Carga(nullableText(d,"chaveNfe"), number(d,"valorNfe"), number(d,"pesoKg"),number(d,"volumeM3"),
                    d.get("volumes") instanceof Number n ? n.intValue() : null, nullableText(d,"janelaEntrega")));
    }
}
