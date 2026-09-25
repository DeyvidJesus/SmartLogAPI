package br.com.smartlog.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Origens autorizadas a chamar a API pelo navegador (CORS).
 * Configurável por {@code smartlog.security.cors.allowed-origins}
 * (variável de ambiente {@code SMARTLOG_CORS_ALLOWED_ORIGINS}, separada por vírgulas).
 */
@ConfigurationProperties(prefix = "smartlog.security.cors")
public record CorsProperties(@DefaultValue("http://localhost:4200") List<String> allowedOrigins) {
}
