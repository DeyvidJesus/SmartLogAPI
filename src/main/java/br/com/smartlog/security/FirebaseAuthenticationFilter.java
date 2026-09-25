package br.com.smartlog.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectProvider<FirebaseAuth> firebaseAuth;
    private final DevAuthProperties devAuth;

    public FirebaseAuthenticationFilter(ObjectProvider<FirebaseAuth> firebaseAuth) {
        this(firebaseAuth, DevAuthProperties.disabled());
    }

    public FirebaseAuthenticationFilter(ObjectProvider<FirebaseAuth> firebaseAuth, DevAuthProperties devAuth) {
        this.firebaseAuth = firebaseAuth;
        this.devAuth = devAuth == null ? DevAuthProperties.disabled() : devAuth;
        if (this.devAuth.enabled()) {
            if (!this.devAuth.hasToken()) {
                throw new IllegalStateException(
                        "smartlog.security.dev-auth.enabled=true exige smartlog.security.dev-auth.token configurado");
            }
            if (!ListRoles.isSupported(this.devAuth.role())) {
                throw new IllegalStateException("smartlog.security.dev-auth.role inválida: " + this.devAuth.role());
            }
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = header.substring(7);
        try {
            AuthenticatedUser user;
            if (devAuth.enabled() && matchesDevToken(rawToken)) {
                user = new AuthenticatedUser(devAuth.uid(), devAuth.empresaId(), devAuth.role());
            } else {
                FirebaseAuth auth = firebaseAuth.getIfAvailable();
                if (auth == null) {
                    // Falha fechada: sem Firebase e sem token de desenvolvimento válido não há autenticação.
                    throw new IllegalArgumentException("Nenhum provedor de autenticação aceitou o token");
                }
                FirebaseToken token = auth.verifyIdToken(rawToken);
                String role = claim(token, "role");
                String empresaId = claim(token, "empresaId");
                if (!ListRoles.isSupported(role) || empresaId == null || empresaId.isBlank()) {
                    throw new IllegalArgumentException("Claims role/empresaId ausentes ou inválidas");
                }
                user = new AuthenticatedUser(token.getUid(), empresaId, role);
            }

            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (FirebaseAuthException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Token inválido ou expirado\",\"timestamp\":\"" + java.time.Instant.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}");
        }
    }

    private boolean matchesDevToken(String rawToken) {
        return MessageDigest.isEqual(
                rawToken.getBytes(StandardCharsets.UTF_8),
                devAuth.token().getBytes(StandardCharsets.UTF_8));
    }

    private static String claim(FirebaseToken token, String name) {
        Object value = token.getClaims().get(name);
        return value == null ? null : value.toString();
    }

    private static final class ListRoles {
        private static boolean isSupported(String role) {
            return "ADMIN".equals(role) || "MOTORISTA".equals(role) || "CLIENTE".equals(role);
        }
    }
}
