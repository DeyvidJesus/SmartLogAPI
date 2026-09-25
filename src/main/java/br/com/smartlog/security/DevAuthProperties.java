package br.com.smartlog.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Autenticação de desenvolvimento (atalho local sem Firebase).
 *
 * <p>Desabilitada por padrão. Só é ativada explicitamente, pelo perfil {@code dev}
 * ({@code application-dev.yml}) ou por {@code smartlog.security.dev-auth.enabled=true}.
 * Quando ativa, somente o token configurado em {@code smartlog.security.dev-auth.token}
 * é aceito e resulta no usuário fixo descrito pelas demais propriedades.
 */
@ConfigurationProperties(prefix = "smartlog.security.dev-auth")
public record DevAuthProperties(
        boolean enabled,
        String token,
        @DefaultValue("admin-dev-001") String uid,
        @DefaultValue("empresa-001") String empresaId,
        @DefaultValue("ADMIN") String role) {

    public static DevAuthProperties disabled() {
        return new DevAuthProperties(false, null, "admin-dev-001", "empresa-001", "ADMIN");
    }

    boolean hasToken() {
        return token != null && !token.isBlank();
    }
}
