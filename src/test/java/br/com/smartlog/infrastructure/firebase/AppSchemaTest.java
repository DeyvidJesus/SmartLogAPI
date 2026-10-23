package br.com.smartlog.infrastructure.firebase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.Timestamp;
class AppSchemaTest {
    private DocumentSnapshot doc(Map<String,Object> fields) {
        var doc=mock(DocumentSnapshot.class);
        when(doc.getId()).thenReturn("aBcD1234");
        when(doc.get(anyString())).thenAnswer(i->fields.get(i.getArgument(0)));
        when(doc.getString(anyString())).thenAnswer(i->(String)fields.get(i.getArgument(0)));
        return doc;
    }
    @Test void flutterDeliveryUsesSameIdTenantCodeAddressAndTimestamp() {
        var at=Instant.parse("2026-10-22T18:00:00Z");
        var d=doc(Map.of("empresaId","tenant-app","id","ENT-ABCD","status","A Caminho",
                "endereco","Destino real do registro","criadoEm",Timestamp.ofTimeSecondsAndNanos(at.getEpochSecond(),0)));
        var e=FirestoreEntregaRepository.toDomain(d);
        assertThat(e.id()).isEqualTo("aBcD1234");
        assertThat(e.codigo()).isEqualTo("ENT-ABCD");
        assertThat(e.empresaId()).isEqualTo("tenant-app");
        assertThat(e.status().name()).isEqualTo("EM_ROTA");
        assertThat(e.destino()).isEqualTo("Destino real do registro");
        assertThat(e.criadaEm()).isEqualTo(at);
    }
    @Test void missingCodeMatchesFlutterAndFunctionContract() {
        assertThat(AppSchema.code(doc(Map.of()))).isEqualTo("ENT-ABCD");
    }
    @Test void mapsRealGpsAndItsFreshnessWithoutInventingPosition() {
        var d=doc(Map.of("empresaId","tenant","nome","Rota","status","Em Rota",
                "localizacaoAtual",Map.of("latitude",-23.55,"longitude",-46.63,"atualizadoEm","2026-10-22T18:00:00Z")));
        var r=FirestoreResourceMapper.rota(d);
        assertThat(r.status()).isEqualTo("EM_ANDAMENTO");
        assertThat(r.localizacaoAtual().latitude()).isEqualTo(-23.55);
        assertThat(r.localizacaoAtual().atualizadoEm()).isEqualTo(Instant.parse("2026-10-22T18:00:00Z"));
        assertThat(FirestoreResourceMapper.rota(doc(Map.of("empresaId","tenant"))).localizacaoAtual()).isNull();
    }
    @Test void routeStatusesRoundTripBetweenDashboardAndDriver() {
        for (var pair : Map.of("PLANEJADA","Pendente","EM_ANDAMENTO","Em Rota","CONCLUIDA","Concluida","PAUSADA","Pausada").entrySet()) {
            assertThat(AppSchema.appRouteStatus(pair.getKey())).isEqualTo(pair.getValue());
            assertThat(AppSchema.routeStatus(pair.getValue())).isEqualTo(pair.getKey());
        }
        assertThat(AppSchema.routeStatus("Concluída")).isEqualTo("CONCLUIDA");
    }
}
