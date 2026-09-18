package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class R9Test {

    private static io.javalin.Javalin app;
    private static String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    public static void setUp() {
        System.setProperty("MODO_TESTE", "1");
        app = Main.startApp(0);
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterAll
    public static void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    public void recusa_conflito_de_sala_sobre_quantidade_de_encontros() throws Exception {
        // Reset
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        // 1. Base activity occupying roomsala-101
        String json1 = "{" +
                "\"titulo\": \"Atividade Base\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [{\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}]" +
                "}";
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1)).build(), HttpResponse.BodyHandlers.ofString());

        // 2. Activity violating both room conflict (1st) and number of encounters for palestra (2nd: 2 encounters)
        String json2 = "{" +
                "\"titulo\": \"Atividade Dupla Conflitante\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T11:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}," +
                "  {\"inicio\": \"2026-10-19T14:00:00-03:00\", \"fim\": \"2026-10-19T15:00:00-03:00\"}" +
                "]" +
                "}";

        HttpResponse<String> resp = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2)).build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(409, resp.statusCode());
        assertTrue(resp.body().contains("CONFLITO_DE_SALA"));
    }

    @Test
    public void recusa_conflito_de_sala_sobre_vagas_acima_da_capacidade() throws Exception {
        // Reset
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        // 1. Base activity occupying room sala-101
        String json1 = "{" +
                "\"titulo\": \"Atividade Base 2\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [{\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}]" +
                "}";
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1)).build(), HttpResponse.BodyHandlers.ofString());

        // 2. Activity violating both room conflict (1st) and vagas above capacity for sala-101 (capacity 40, vagas 50) (3rd)
        String json2 = "{" +
                "\"titulo\": \"Atividade Lotada Conflitante\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 50," +
                "\"encontros\": [{\"inicio\": \"2026-10-19T11:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}]" +
                "}";

        HttpResponse<String> resp = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2)).build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(409, resp.statusCode());
        assertTrue(resp.body().contains("CONFLITO_DE_SALA"));
    }

    @Test
    public void recusa_quantidade_de_encontros_sobre_vagas_acima_da_capacidade() throws Exception {
        // Reset
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        // Activity violating both number of encounters (palestra with 2 encounters) (2nd)
        // AND vagas above capacity for sala-101 (capacity 40, vagas 50) (3rd)
        // Precedence: 2nd should trigger QUANTIDADE_DE_ENCONTROS (422) over VAGAS_ACIMA_DA_CAPACIDADE (422)
        String json = "{" +
                "\"titulo\": \"Palestra Dupla Lotada\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 50," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T14:00:00-03:00\", \"fim\": \"2026-10-19T15:00:00-03:00\"}," +
                "  {\"inicio\": \"2026-10-19T16:00:00-03:00\", \"fim\": \"2026-10-19T17:00:00-03:00\"}" +
                "]" +
                "}";

        HttpResponse<String> resp = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("QUANTIDADE_DE_ENCONTROS"));
        assertFalse(resp.body().contains("VAGAS_ACIMA_DA_CAPACIDADE"));
    }

    @Test
    public void recusa_encontro_invalido_sobre_vagas_acima_da_capacidade() throws Exception {
        // Reset
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        // Activity violating both invalid encounter duration (5 hours) (2nd)
        // AND vagas above capacity for sala-101 (capacity 40, vagas 50) (3rd)
        // Precedence: 2nd should trigger ENCONTRO_INVALIDO (422) over VAGAS_ACIMA_DA_CAPACIDADE (422)
        String json = "{" +
                "\"titulo\": \"Palestra Longa Lotada\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 50," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T15:00:00-03:00\"}" +
                "]" +
                "}";

        HttpResponse<String> resp = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("ENCONTRO_INVALIDO"));
        assertFalse(resp.body().contains("VAGAS_ACIMA_DA_CAPACIDADE"));
    }
}
