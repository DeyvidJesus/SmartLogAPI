package br.com.smartlog.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.cloud.firestore.DocumentSnapshot;

import br.com.smartlog.domain.model.Veiculo;

class FirestoreVeiculoRepositoryTest {

    private final FirestoreStore store = mock(FirestoreStore.class);
    private final FirestoreVeiculoRepository repository = new FirestoreVeiculoRepository(store);
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    @SuppressWarnings("unchecked")
    void saveWritesVeiculoUnderTenantCollection() {
        Veiculo veiculo = new Veiculo("veic-1", "empresa-1", "ABC1D23", "Accelo", "VUC", 5200.0, 24.0, 2024, true, now, now);

        repository.save(veiculo);

        ArgumentCaptor<Map<String, Object>> data = ArgumentCaptor.forClass(Map.class);
        verify(store).save(eq("empresa-1"), eq("veiculos"), eq("veic-1"), data.capture());
        assertThat(data.getValue())
                .containsEntry("placa", "ABC1D23")
                .containsEntry("capacidadeKg", 5200.0)
                .containsEntry("ano", 2024)
                .containsEntry("ativo", true)
                .containsEntry("criadaEm", now.toString())
                .doesNotContainKey("empresaId");
    }

    @Test
    void findByIdMapsDocumentToDomain() {
        DocumentSnapshot document = mock(DocumentSnapshot.class, RETURNS_DEEP_STUBS);
        when(document.getId()).thenReturn("veic-1");
        when(document.getReference().getParent().getParent().getId()).thenReturn("empresa-1");
        when(document.getString("placa")).thenReturn("ABC1D23");
        when(document.getString("modelo")).thenReturn("Accelo");
        when(document.getString("tipo")).thenReturn("VUC");
        when(document.getDouble("capacidadeKg")).thenReturn(5200.0);
        when(document.getDouble("capacidadeM3")).thenReturn(24.0);
        when(document.getLong("ano")).thenReturn(2024L);
        when(document.getBoolean("ativo")).thenReturn(true);
        when(document.getString("criadaEm")).thenReturn(now.toString());
        when(document.getString("atualizadaEm")).thenReturn(now.toString());
        when(store.find("empresa-1", "veiculos", "veic-1")).thenReturn(Optional.of(document));

        Optional<Veiculo> result = repository.findById("empresa-1", "veic-1");

        assertThat(result).contains(
                new Veiculo("veic-1", "empresa-1", "ABC1D23", "Accelo", "VUC", 5200.0, 24.0, 2024, true, now, now));
    }
}
