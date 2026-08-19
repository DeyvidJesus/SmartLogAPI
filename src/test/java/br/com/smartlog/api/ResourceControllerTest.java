package br.com.smartlog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import br.com.smartlog.application.port.ClienteRepository;
import br.com.smartlog.application.port.MotoristaRepository;
import br.com.smartlog.application.port.RotaRepository;
import br.com.smartlog.domain.model.Cliente;
import br.com.smartlog.domain.model.Motorista;
import br.com.smartlog.domain.model.Rota;
import br.com.smartlog.security.AuthenticatedUser;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean RotaRepository rotas;
    @MockBean ClienteRepository clientes;
    @MockBean MotoristaRepository motoristas;

    @Test
    void clientCannotAccessAdministrativeResources() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                .with(authentication(tokenFor("CLIENTE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Cliente\",\"email\":\"cliente@example.com\",\"ativo\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidClientRequestReturnsStructuredBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                .with(authentication(tokenFor("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"invalido\",\"ativo\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanCreateRoute() throws Exception {
        when(rotas.save(any(Rota.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(post("/api/v1/rotas")
                .with(authentication(tokenFor("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"ABC\",\"motoristaId\":\"m1\",\"status\":\"PLANEJADA\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCanCreateDriver() throws Exception {
        when(motoristas.save(any(Motorista.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(post("/api/v1/motoristas")
                .with(authentication(tokenFor("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"uid\":\"u1\",\"nome\":\"João\",\"email\":\"joao@example.com\",\"ativo\":true}"))
                .andExpect(status().isCreated());
    }

    @SuppressWarnings("unchecked")
    private static UsernamePasswordAuthenticationToken tokenFor(String role) {
        AuthenticatedUser user = new AuthenticatedUser("uid-1", "empresa-1", role);
        return new UsernamePasswordAuthenticationToken(user, null, user.authorities());
    }
}
