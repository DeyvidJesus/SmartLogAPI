package br.com.smartlog.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.smartlog.application.port.EntregaRepository;
import br.com.smartlog.domain.model.Entrega;
import br.com.smartlog.domain.model.EntregaStatus;
import br.com.smartlog.security.AuthenticatedUser;

@SpringBootTest
@AutoConfigureMockMvc
class EntregaControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    EntregaRepository repository;
    @MockBean br.com.smartlog.application.port.RotaRepository rotas;
    @MockBean br.com.smartlog.application.port.ClienteRepository clientes;

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/entregas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientCannotCreateEntrega() throws Exception {
        mockMvc.perform(post("/api/v1/entregas")
                .with(authentication(tokenFor("CLIENTE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":\"c1\",\"rotaId\":\"r1\",\"origem\":\"SP\",\"destino\":\"ABC\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateEntrega() throws Exception {
        var now=Instant.now();
        when(rotas.findById("empresa-1","r1")).thenReturn(java.util.Optional.of(new br.com.smartlog.domain.model.Rota("r1","empresa-1","Rota","m1","Pendente",now,now)));
        when(clientes.findById("empresa-1","c1")).thenReturn(java.util.Optional.of(new br.com.smartlog.domain.model.Cliente("c1","empresa-1","Cliente","c@example.test","",true,now,now)));
        when(repository.save(any(Entrega.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/entregas")
                .with(authentication(tokenFor("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":\"c1\",\"rotaId\":\"r1\",\"origem\":\"SP\",\"destino\":\"ABC\"}"))
                .andExpect(status().isCreated());
    }

    private static UsernamePasswordAuthenticationToken tokenFor(String role) {
        AuthenticatedUser user = new AuthenticatedUser("uid-1", "empresa-1", role);
        return new UsernamePasswordAuthenticationToken(user, null, user.authorities());
    }
}
