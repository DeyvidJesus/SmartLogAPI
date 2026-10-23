package br.com.smartlog.security;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseToken;
class FirebaseUserDirectoryTest {
    private final Firestore db=mock(Firestore.class,RETURNS_DEEP_STUBS);
    private final com.google.cloud.firestore.DocumentSnapshot document=mock(com.google.cloud.firestore.DocumentSnapshot.class);
    @org.junit.jupiter.api.BeforeEach void setup() {
        when(db.collection("usuarios").document("uid").get()).thenReturn(com.google.api.core.ApiFutures.immediateFuture(document));
    }
    private FirebaseUserDirectory directory() { return new FirebaseUserDirectory(new StaticListableBeanFactory(Map.of("db",db)).getBeanProvider(Firestore.class)); }
    private FirebaseToken token() { var token=mock(FirebaseToken.class);when(token.getUid()).thenReturn("uid");when(token.getClaims()).thenReturn(Map.of());return token; }
    @Test void resolvesLucasAdministratorFromVerifiedTokenAndServerProfile() throws Exception {

        when(document.exists()).thenReturn(true);
        when(document.getData()).thenReturn(Map.of("tipoUsuario","ADMINISTRADOR","empresaId","tenant-lucas"));
        assertThat(directory().resolve(token())).isEqualTo(new AuthenticatedUser("uid","tenant-lucas","ADMIN"));
    }
    @Test void missingProfileCannotBecomeDefaultAdmin() throws Exception {
        when(document.exists()).thenReturn(false);
        assertThatThrownBy(()->directory().resolve(token())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void disabledAccountIsRejected() throws Exception {

        when(document.exists()).thenReturn(true);
        when(document.getData()).thenReturn(Map.of("tipoUsuario","MOTORISTA","empresaId","tenant","ativo",false));
        assertThatThrownBy(()->directory().resolve(token())).isInstanceOf(IllegalArgumentException.class);
    }
}
