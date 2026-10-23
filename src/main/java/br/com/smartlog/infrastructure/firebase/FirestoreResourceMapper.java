package br.com.smartlog.infrastructure.firebase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.google.cloud.firestore.DocumentSnapshot;

import br.com.smartlog.domain.model.Cliente;
import br.com.smartlog.domain.model.Motorista;
import br.com.smartlog.domain.model.Rota;
import br.com.smartlog.domain.model.Veiculo;

final class FirestoreResourceMapper {
    private FirestoreResourceMapper() { }

    static Rota rota(DocumentSnapshot d) {
        return new Rota(d.getId(), tenant(d), d.getString("nome"), d.getString("motoristaId"),
                AppSchema.routeStatus(d.getString("status")), instant(d, "criadaEm"), instant(d, "atualizadaEm"), location(d));
    }
    static Cliente cliente(DocumentSnapshot d) {
        return new Cliente(d.getId(), tenant(d), d.getString("nome"), d.getString("email"),
                d.getString("telefone"), AppSchema.active(d),
                instant(d, "criadaEm"), instant(d, "atualizadaEm"));
    }
    static Motorista motorista(DocumentSnapshot d) {
        return new Motorista(d.getId(), tenant(d), AppSchema.text(d, "uid") .isBlank() ? d.getId() : d.getString("uid"), d.getString("nome"),
                d.getString("email"), AppSchema.active(d),
                instant(d, "criadaEm"), instant(d, "atualizadaEm"));
    }
    static Veiculo veiculo(DocumentSnapshot d) {
        Long ano = d.getLong("ano");
        return new Veiculo(d.getId(), tenant(d), d.getString("placa"), d.getString("modelo"),
                d.getString("tipo"), d.getDouble("capacidadeKg"), d.getDouble("capacidadeM3"),
                ano == null ? null : ano.intValue(), AppSchema.active(d),
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
    static Map<String, Object> veiculoMap(Veiculo v) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", v.id()); m.put("placa", v.placa()); m.put("modelo", v.modelo()); m.put("tipo", v.tipo());
        m.put("capacidadeKg", v.capacidadeKg()); m.put("capacidadeM3", v.capacidadeM3()); m.put("ano", v.ano());
        m.put("ativo", v.ativo()); m.put("criadaEm", v.criadaEm().toString()); m.put("atualizadaEm", v.atualizadaEm().toString());
        return m;
    }
    private static Rota.Localizacao location(DocumentSnapshot d) {
        Object raw = d.get("localizacaoAtual");
        if (!(raw instanceof Map<?,?> data)) return null;
        if (!(data.get("latitude") instanceof Number lat) || !(data.get("longitude") instanceof Number lng)) return null;
        if (Math.abs(lat.doubleValue()) > 90 || Math.abs(lng.doubleValue()) > 180) return null;
        Object time = data.get("atualizadoEm");
        Instant at = time instanceof com.google.cloud.Timestamp timestamp ? timestamp.toDate().toInstant() : null;
        if (time instanceof String text) { try { at = Instant.parse(text); } catch (java.time.format.DateTimeParseException ignored) { } }
        return new Rota.Localizacao(lat.doubleValue(), lng.doubleValue(), at);
    }
    private static String tenant(DocumentSnapshot d) { return d.getString("empresaId"); }
    private static Instant instant(DocumentSnapshot d, String field) { return AppSchema.instant(d, field, "criadoEm", "dataCadastro"); }
}
