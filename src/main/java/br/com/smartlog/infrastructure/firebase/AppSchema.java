package br.com.smartlog.infrastructure.firebase;
import java.text.Normalizer;
import java.time.Instant;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import br.com.smartlog.domain.model.EntregaStatus;
/** Adapts Flutter's production schema without changing document identities. */
public final class AppSchema {
    private AppSchema() { }
    public static String text(DocumentSnapshot doc, String... fields) {
        for (String field : fields) {
            Object value = doc.get(field);
            if (value != null && !value.toString().isBlank()) return value.toString();
        }
        return "";
    }
    public static Instant instant(DocumentSnapshot doc, String... fields) {
        for (String field : fields) {
            Object value = doc.get(field);
            if (value instanceof Timestamp time) return time.toDate().toInstant();
            if (value instanceof String text) {
                try { return Instant.parse(text); } catch (java.time.format.DateTimeParseException ignored) { }
            }
        }
        return Instant.EPOCH;
    }
    public static String key(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .trim().toUpperCase().replace(' ', '_');
    }
    public static EntregaStatus status(String text) {
        return switch (key(text)) {
            case "A_CAMINHO", "EM_ROTA", "EM_TRANSITO" -> EntregaStatus.EM_ROTA;
            case "ENTREGUE" -> EntregaStatus.ENTREGUE;
            case "ATRASADA" -> EntregaStatus.ATRASADA;
            case "CANCELADA", "CANCELADO" -> EntregaStatus.CANCELADA;
            case "PENDENTE", "" -> EntregaStatus.PENDENTE;
            default -> throw new br.com.smartlog.domain.exception.PersistenceException("Status de entrega desconhecido", null);
        };
    }
    public static String appStatus(EntregaStatus status) {
        return switch (status) {
            case PENDENTE -> "Pendente"; case EM_ROTA -> "Em Rota";
            case ENTREGUE -> "Entregue"; case CANCELADA -> "Cancelada"; case ATRASADA -> "Atrasada";
        };
    }
    public static String routeStatus(String status) {
        return switch (key(status == null ? "" : status)) {
            case "", "PENDENTE", "PLANEJADA" -> "PLANEJADA";
            case "A_CAMINHO", "EM_ROTA", "EM_TRANSITO", "EM_ANDAMENTO" -> "EM_ANDAMENTO";
            case "CONCLUIDA", "FINALIZADA", "ENTREGUE" -> "CONCLUIDA";
            case "PAUSADA" -> "PAUSADA";
            case "CANCELADA", "CANCELADO" -> "CANCELADA";
            default -> status;
        };
    }
    public static String appRouteStatus(String status) {
        return switch (routeStatus(status)) {
            case "PLANEJADA" -> "Pendente";
            case "EM_ANDAMENTO" -> "Em Rota";
            case "CONCLUIDA" -> "Concluida";
            case "PAUSADA" -> "Pausada";
            case "CANCELADA" -> "Cancelada";
            default -> status;
        };
    }
    public static String code(DocumentSnapshot doc) {
        String code = text(doc, "codigo", "id");
        return code.startsWith("ENT-") ? code : "ENT-" + doc.getId().substring(0, Math.min(4, doc.getId().length())).toUpperCase();
    }
    public static boolean belongs(DocumentSnapshot doc, String tenant) {
        return doc.exists() && tenant.equals(doc.getString("empresaId"));
    }
    public static boolean active(DocumentSnapshot doc) {
        return !Boolean.FALSE.equals(doc.get("ativo")) && !"INATIVO".equals(key(text(doc, "status")));
    }
}
