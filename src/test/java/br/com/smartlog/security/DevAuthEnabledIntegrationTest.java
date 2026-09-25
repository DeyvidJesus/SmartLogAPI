package br.com.smartlog.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Com o perfil dev, apenas o token configurado em application-dev.yml é aceito. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevAuthEnabledIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void configuredDevTokenIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/veiculos").header("Authorization", "Bearer smartlog-dev-token"))
                .andExpect(status().isOk());
    }

    @Test
    void wrongTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/veiculos").header("Authorization", "Bearer admin-bearer-token-smartlog"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/veiculos"))
                .andExpect(status().isUnauthorized());
    }
}
