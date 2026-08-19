package br.com.smartlog.infrastructure.firebase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.google.cloud.firestore.DocumentSnapshot;

import br.com.smartlog.domain.model.Cliente;
import br.com.smartlog.domain.model.Motorista;
import br.com.smartlog.domain.model.Rota;

final class FirestoreResourceMapper {
    private FirestoreResourceMapper() { }

    static Rota rota(DocumentSnapshot d) {
        return new Rota(d.getId(), tenant(d), d.getString("nome"), d.getString("motoristaId"),
                d.getString("status"), instant(d, "criadaEm"), instant(d, "atualizadaEm"));
    }
    static Cliente cliente(DocumentSnapshot d) {
        return new Cliente(d.getId(), tenant(d), d.getString("nome"), d.getString("email"),
                d.getString("telefone"), Boolean.TRUE.equals(d.getBoolean("ativo")),
                instant(d, "criadaEm"), instant(d, "atualizadaEm"));
    }
    static Motorista motorista(DocumentSnapshot d) {
        return new Motorista(d.getId(), tenant(d), d.getString("uid"), d.getString("nome"),
                d.getString("email"), Boolean.TRUE.equals(d.getBoolean("ativo")),
                instant(d, "criadaEm"), instant(d, "atualizadaEm"));
    }
    static Map<String, Object> map(String id, String nome, String motoristaId, String status,
            Instant criada, Instant atualizada) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id); m.put("nome", nome); m.put("motoristaId", motoristaId);
        m.put("status", status); m.put("criadaEm", criada.toString()); m.put("atualizadaEm", atualizada.toString());
        return m;
    }
    static Map<String, Object> map(String id, String nome, String email, String telefone,
            boolean ativo, Instant criada, Instant atualizada) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id); m.put("nome", nome); m.put("email", email); m.put("telefone", telefone);
        m.put("ativo", ativo); m.put("criadaEm", criada.toString()); m.put("atualizadaEm", atualizada.toString());
        return m;
    }
    static Map<String, Object> motoristaMap(String id, String uid, String nome, String email,
            boolean ativo, Instant criada, Instant atualizada) {
        Map<String, Object> m = map(id, nome, email, null, ativo, criada, atualizada);
        m.put("uid", uid); return m;
    }
    private static String tenant(DocumentSnapshot d) { return d.getReference().getParent().getParent().getId(); }
    private static Instant instant(DocumentSnapshot d, String field) { return Instant.parse(d.getString(field)); }
}
