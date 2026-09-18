package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class R8Test {

    private static io.javalin.Javalin app;
    private static String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

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
    public void GET_atividades_filtra_dia_e_tipo_com_and_inclui_canceladas_e_ordena() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        // 1. Create Activity 1: "B - Palestra Beta" (palestra, 2026-10-19 10:00)
        String json1 = "{" +
                "\"titulo\": \"B - Palestra Beta\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 20," +
                "\"encontros\": [{\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}]" +
                "}";
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1)).build(), HttpResponse.BodyHandlers.ofString());

        // 2. Create Activity 2: "A - Palestra Alfa" (palestra, 2026-10-19 10:00) - and cancel it
        String json2 = "{" +
                "\"titulo\": \"A - Palestra Alfa\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-102\"," +
                "\"vagas\": 20," +
                "\"encontros\": [{\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}]" +
                "}";
        HttpResponse<String> resp2 = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2)).build(), HttpResponse.BodyHandlers.ofString());
        Map map2 = mapper.readValue(resp2.body(), Map.class);
        String id2 = (String) map2.get("id");

        // Cancel Activity 2
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades/" + id2 + "/cancelamento"))
                .header("X-Usuario", "org-ana")
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        // 3. Create Activity 3: "Minicurso Gama" (minicurso, 2026-10-19 10:00)
        String json3 = "{" +
                "\"titulo\": \"Minicurso Gama\"," +
                "\"tipo\": \"minicurso\"," +
                "\"salaId\": \"lab-3\"," +
                "\"vagas\": 20," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T14:00:00-03:00\", \"fim\": \"2026-10-19T16:00:00-03:00\"}," +
                "  {\"inicio\": \"2026-10-20T14:00:00-03:00\", \"fim\": \"2026-10-20T16:00:00-03:00\"}" +
                "]" +
                "}";
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json3)).build(), HttpResponse.BodyHandlers.ofString());

        // Call GET /atividades?dia=2026-10-19&tipo=palestra
        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades?dia=2026-10-19&tipo=palestra"))
                .header("X-Usuario", "p-carla")
                .GET()
                .build();

        HttpResponse<String> respGet = client.send(reqGet, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respGet.statusCode());

        List<Map<String, Object>> list = mapper.readValue(respGet.body(), List.class);
        // Should contain 2 activities: "A - Palestra Alfa" (canceled) and "B - Palestra Beta"
        // Sorted by start time (both 10:00), then by title alphabetically -> "A - Palestra Alfa" first, then "B - Palestra Beta"
        assertEquals(2, list.size());
        assertEquals("A - Palestra Alfa", list.get(0).get("titulo"));
        assertEquals("cancelada", list.get(0).get("situacao"));
        assertEquals("B - Palestra Beta", list.get(1).get("titulo"));
        assertEquals("prevista", list.get(1).get("situacao"));
    }
}
