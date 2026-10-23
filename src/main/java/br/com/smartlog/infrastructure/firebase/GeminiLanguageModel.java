package br.com.smartlog.infrastructure.firebase;
import java.time.Duration;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.smartlog.application.port.LanguageModel;
/** Key lives only in the server environment. No paid-provider fallback. */
@Component
public class GeminiLanguageModel implements LanguageModel {
    private final String key;
    private final String model;
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public GeminiLanguageModel(@Value("${GEMINI_API_KEY:}") String key,
            @Value("${GEMINI_MODEL:gemini-3.1-flash-lite}") String model, ObjectMapper json) {
        this.key = key; this.model = model; this.json = json;
    }
    public String answer(String instructions, String context, String question) {
        if (key.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Configure GEMINI_API_KEY no servidor para ativar o assistente.");
        try {
            String body = json.writeValueAsString(Map.of("model", model, "messages", List.of(
                    Map.of("role", "system", "content", instructions),
                    Map.of("role", "user", "content", "DADOS DA OPERAÇÃO:\n" + context + "\nPERGUNTA:\n" + question)),
                    "max_tokens", 900));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                    .timeout(Duration.ofSeconds(35)).header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + key).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Quota do assistente atingida. Tente mais tarde.");
            if (response.statusCode() == 401 || response.statusCode() == 403)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Credencial do assistente indisponível.");
            if (response.statusCode() != 200) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Provedor de IA indisponível.");
            String answer = json.readTree(response.body()).path("choices").path(0).path("message").path("content").asText("").trim();
            if (answer.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O assistente não retornou conteúdo.");
            return answer;
        } catch (ResponseStatusException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Consulta interrompida."); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o assistente."); }
    }
}
