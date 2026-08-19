package br.com.smartlog.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI smartlogOpenAPI() {
        return new OpenAPI().info(new Info().title("SmartLog API").version("v1")
                .description("API REST de operações logísticas com isolamento por empresaId."))
                .components(new Components().addSecuritySchemes("firebaseBearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("Firebase ID Token")))
                .addSecurityItem(new SecurityRequirement().addList("firebaseBearer"));
    }
}
