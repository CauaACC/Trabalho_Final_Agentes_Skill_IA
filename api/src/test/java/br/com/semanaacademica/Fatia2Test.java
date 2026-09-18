package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class Fatia2Test {

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
    public void testPostPalestraWithTwoEncontrosReturns422QuantidadeDeEncontros() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        String jsonBody = "{" +
                "\"titulo\": \"Palestra Dupla\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"auditorio\"," +
                "\"vagas\": 50," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T11:00:00-03:00\"}," +
                "  {\"inicio\": \"2026-10-19T14:00:00-03:00\", \"fim\": \"2026-10-19T15:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("QUANTIDADE_DE_ENCONTROS"));
    }

    @Test
    public void testPostMinicursoWith5HoursEncontroReturns422EncontroInvalido() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        String jsonBody = "{" +
                "\"titulo\": \"Minicurso Longo\"," +
                "\"tipo\": \"minicurso\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 20," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T08:00:00-03:00\", \"fim\": \"2026-10-19T13:00:00-03:00\"}," +
                "  {\"inicio\": \"2026-10-20T08:00:00-03:00\", \"fim\": \"2026-10-20T10:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("ENCONTRO_INVALIDO"));
    }

    @Test
    public void testPostVagasAcimaDaCapacidadeReturns422VagasAcimaDaCapacidade() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        String jsonBody = "{" +
                "\"titulo\": \"Palestra Lotação\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 50," + // Sala 101 capacidade é 40
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T11:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("VAGAS_ACIMA_DA_CAPACIDADE"));
    }

    @Test
    public void testPostConflitoSalaReturns409ConflitoDeSala() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        // First activity
        String jsonBody1 = "{" +
                "\"titulo\": \"Palestra 1\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody1))
                .build();
        client.send(req1, HttpResponse.BodyHandlers.ofString());

        // Conflicting activity (overlapping time in sala-101)
        String jsonBody2 = "{" +
                "\"titulo\": \"Palestra Conflitante\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T11:00:00-03:00\", \"fim\": \"2026-10-19T13:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody2))
                .build();

        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, resp2.statusCode());
        assertTrue(resp2.body().contains("CONFLITO_DE_SALA"));
    }

    @Test
    public void testPostPrecedenciaConflitoSobreQuantidadeReturns409() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        // First activity occupying room
        String jsonBody1 = "{" +
                "\"titulo\": \"Palestra Base\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody1))
                .build();
        client.send(req1, HttpResponse.BodyHandlers.ofString());

        // Second activity: violates BOTH room conflict AND number of encounters (palestra with 2 encounters)
        // Precedence R9 says: 1º conflito de sala/horário (409 CONFLITO_DE_SALA)
        String jsonBody2 = "{" +
                "\"titulo\": \"Palestra Violacoes\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T11:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}," +
                "  {\"inicio\": \"2026-10-19T14:00:00-03:00\", \"fim\": \"2026-10-19T15:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody2))
                .build();

        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, resp2.statusCode());
        assertTrue(resp2.body().contains("CONFLITO_DE_SALA"));
    }
}
