package br.com.smartlog.security;

import java.io.IOException;

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

    public FirebaseAuthenticationFilter(ObjectProvider<FirebaseAuth> firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            FirebaseAuth auth = firebaseAuth.getIfAvailable();
            AuthenticatedUser user;

            if (auth != null) {
                FirebaseToken token = auth.verifyIdToken(header.substring(7));
                String role = claim(token, "role");
                String empresaId = claim(token, "empresaId");
                if (!ListRoles.isSupported(role) || empresaId == null || empresaId.isBlank()) {
                    throw new IllegalArgumentException("Claims role/empresaId ausentes ou inválidas");
                }
                user = new AuthenticatedUser(token.getUid(), empresaId, role);
            } else {
                // Modo dev local sem Firebase SDK configurado
                String rawToken = header.substring(7);
                String role = "ADMIN";
                String empresaId = "empresa-001";
                String uid = "admin-dev-001";
                if (rawToken.contains(":")) {
                    String[] parts = rawToken.split(":");
                    if (parts.length >= 3) {
                        uid = parts[0];
                        empresaId = parts[1];
                        role = parts[2].toUpperCase();
                    }
                } else if (rawToken.toUpperCase().contains("MOTORISTA")) {
                    role = "MOTORISTA";
                    uid = "motorista-001";
                } else if (rawToken.toUpperCase().contains("CLIENTE")) {
                    role = "CLIENTE";
                    uid = "cliente-001";
                }
                user = new AuthenticatedUser(uid, empresaId, role);
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
