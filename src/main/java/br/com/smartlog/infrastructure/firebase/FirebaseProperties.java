package br.com.smartlog.infrastructure.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(boolean enabled, String projectId) {
}
