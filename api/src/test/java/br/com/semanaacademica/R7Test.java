package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class R7Test {

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
    public void calcula_carga_horaria_e_situacao_dinamica_conforme_relogio_e_cancelamento() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        // Create a minicurso with 2 encounters of 2 hours each (120 mins each = 240 mins total)
        String jsonCreate = "{" +
                "\"titulo\": \"Minicurso R7\"," +
                "\"tipo\": \"minicurso\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T09:00:00-03:00\", \"fim\": \"2026-10-19T11:00:00-03:00\"}," +
                "  {\"inicio\": \"2026-10-19T13:00:00-03:00\", \"fim\": \"2026-10-19T15:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest reqCreate = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCreate))
                .build();

        HttpResponse<String> respCreate = client.send(reqCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respCreate.statusCode());

        Map map = mapper.readValue(respCreate.body(), Map.class);
        String atvId = (String) map.get("id");

        // 1. Carga horária should be 240
        assertEquals(240, ((Number) map.get("cargaHorariaMinutos")).intValue());

        // Set clock before 1st encounter: 2026-10-19T08:00:00-03:00 -> prevista
        setClock("2026-10-19T08:00:00-03:00");
        Map detailPrevista = getAtividade(atvId);
        assertEquals("prevista", detailPrevista.get("situacao"));

        // Set clock during 1st encounter / between encounters: 2026-10-19T10:00:00-03:00 -> em_andamento
        setClock("2026-10-19T10:00:00-03:00");
        Map detailEmAndamento = getAtividade(atvId);
        assertEquals("em_andamento", detailEmAndamento.get("situacao"));

        // Set clock after last encounter: 2026-10-19T16:00:00-03:00 -> encerrada
        setClock("2026-10-19T16:00:00-03:00");
        Map detailEncerrada = getAtividade(atvId);
        assertEquals("encerrada", detailEncerrada.get("situacao"));

        // Reset clock to before start and cancel activity -> cancelada prevails even if clock changes or doesn't
        setClock("2026-10-19T08:00:00-03:00");
        HttpRequest reqCancel = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId + "/cancelamento"))
                .header("X-Usuario", "org-ana")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respCancel = client.send(reqCancel, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respCancel.statusCode());

        Map detailCancelada = mapper.readValue(respCancel.body(), Map.class);
        assertEquals("cancelada", detailCancelada.get("situacao"));
    }

    private void setClock(String isoDateTime) throws Exception {
        String json = "{\"agora\": \"" + isoDateTime + "\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private Map getAtividade(String id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + id))
                .header("X-Usuario", "p-carla")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(resp.body(), Map.class);
    }
}
