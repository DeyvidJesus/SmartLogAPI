package br.com.smartlog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerDocumentationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void openApiDocumentIsPublicAndContainsPriorityResources() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SmartLog API"))
                .andExpect(jsonPath("$.paths['/api/v1/entregas']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/rotas']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/clientes']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/motoristas']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.firebaseBearer").exists());
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
