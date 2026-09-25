package br.com.smartlog.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenCreatesAuthenticatedUser() throws Exception {
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(auth.verifyIdToken("token")).thenReturn(token);
        when(token.getUid()).thenReturn("uid-1");
        when(token.getClaims()).thenReturn(Map.of("role", "MOTORISTA", "empresaId", "empresa-1"));

        ObjectProvider<FirebaseAuth> provider = new FixedProvider<>(auth);
        FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_MOTORISTA");
        verify(auth).verifyIdToken("token");
    }

    @Test
    void anyBearerTokenIsRejectedWhenFirebaseAndDevAuthAreDisabled() throws Exception {
        FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter(new FixedProvider<>(null));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithBearer("qualquer-token"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void configuredDevTokenAuthenticatesDevUserWhenDevAuthIsEnabled() throws Exception {
        FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter(new FixedProvider<>(null),
                devAuth("token-dev"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithBearer("token-dev"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        AuthenticatedUser user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        assertThat(user).isEqualTo(new AuthenticatedUser("admin-dev-001", "empresa-001", "ADMIN"));
    }

    @Test
    void wrongTokenIsRejectedWhenDevAuthIsEnabled() throws Exception {
        FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter(new FixedProvider<>(null),
                devAuth("token-dev"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithBearer("uid:empresa-001:ADMIN"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void devAuthEnabledWithoutTokenFailsFast() {
        assertThatThrownBy(() -> new FirebaseAuthenticationFilter(new FixedProvider<>(null), devAuth(" ")))
                .isInstanceOf(IllegalStateException.class);
    }

    private static DevAuthProperties devAuth(String token) {
        return new DevAuthProperties(true, token, "admin-dev-001", "empresa-001", "ADMIN");
    }

    private static MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private record FixedProvider<T>(T value) implements ObjectProvider<T> {
        @Override public T getObject(Object... args) { return value; }
        @Override public T getIfAvailable() { return value; }
        @Override public T getIfUnique() { return value; }
        @Override public T getObject() { return value; }
    }
}
