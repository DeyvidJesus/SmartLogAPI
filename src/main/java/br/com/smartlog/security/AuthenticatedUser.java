package br.com.smartlog.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(String uid, String empresaId, String role) {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isMotorista() { return "MOTORISTA".equals(role); }
    public boolean isCliente() { return "CLIENTE".equals(role); }
}
