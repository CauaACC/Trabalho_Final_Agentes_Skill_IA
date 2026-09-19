package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class M2Fatia3Test {

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
    public void testInscricoesEncerradas30MinutosAntes() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        HttpRequest clockReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"agora\": \"2026-10-19T09:35:00-03:00\"}"))
                .build();
        client.send(clockReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_enc', 'Atividade Fechamento', 'palestra', 'auditorio', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_enc', 'atv_enc', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_enc/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("INSCRICOES_ENCERRADAS"));
    }

    @Test
    public void testCancelamentoInscricaoESucessosEErros() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        HttpRequest clockReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"agora\": \"2026-10-19T08:00:00-03:00\"}"))
                .build();
        client.send(clockReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_can', 'Atividade Cancelamento', 'palestra', 'auditorio', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_can', 'atv_can', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
        }

        HttpRequest reqSub = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_can/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respSub = client.send(reqSub, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respSub.statusCode());

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map map = mapper.readValue(respSub.body(), java.util.Map.class);
        String insId = (String) map.get("id");

        HttpRequest clockAdvance = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"agora\": \"2026-10-19T10:05:00-03:00\"}"))
                .build();
        client.send(clockAdvance, HttpResponse.BodyHandlers.ofString());

        HttpRequest reqCancelStarted = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + insId + "/cancelamento"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respCancelStarted = client.send(reqCancelStarted, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, respCancelStarted.statusCode());
        assertTrue(respCancelStarted.body().contains("ATIVIDADE_JA_INICIADA"));

        client.send(clockReq, HttpResponse.BodyHandlers.ofString());

        HttpRequest reqCancelOk = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + insId + "/cancelamento"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respCancelOk = client.send(reqCancelOk, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respCancelOk.statusCode());
        assertTrue(respCancelOk.body().contains("cancelada"));

        HttpRequest reqCancelAgain = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + insId + "/cancelamento"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respCancelAgain = client.send(reqCancelAgain, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, respCancelAgain.statusCode());
        assertTrue(respCancelAgain.body().contains("INSCRICAO_INATIVA"));
    }

    @Test
    public void testAcessarInscricaoOutroParticipante404() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_sec', 'Atividade Seguranca', 'palestra', 'auditorio', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_sec', 'atv_sec', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
        }

        HttpRequest reqSub = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_sec/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respSub = client.send(reqSub, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respSub.statusCode());

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map map = mapper.readValue(respSub.body(), java.util.Map.class);
        String insId = (String) map.get("id");

        HttpRequest reqGet = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + insId))
                .header("X-Usuario", "p-diego")
                .GET()
                .build();
        HttpResponse<String> respGet = client.send(reqGet, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, respGet.statusCode());
        assertFalse(respGet.body().contains("403"));
    }
}
