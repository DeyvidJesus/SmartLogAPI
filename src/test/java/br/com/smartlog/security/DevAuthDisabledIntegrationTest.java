package br.com.smartlog.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Sem Firebase e sem perfil dev, nenhum Bearer token é aceito (falha fechada). */
@SpringBootTest
@AutoConfigureMockMvc
class DevAuthDisabledIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void arbitraryBearerTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/veiculos").header("Authorization", "Bearer qualquer-coisa"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devTokenIsNotAcceptedOutsideDevProfile() throws Exception {
        mockMvc.perform(get("/api/v1/veiculos").header("Authorization", "Bearer smartlog-dev-token"))
                .andExpect(status().isUnauthorized());
    }
}
