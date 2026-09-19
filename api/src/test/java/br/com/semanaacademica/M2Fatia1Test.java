package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class M2Fatia1Test {

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
    public void testInscricaoSomenteParticipante() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_ins1', 'Palestra Teste', 'palestra', 'auditorio', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_ins1', 'atv_ins1', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_ins1/inscricoes"))
                .header("X-Usuario", "org-ana") // Organization user, not participant
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
        assertTrue(response.body().contains("SOMENTE_PARTICIPANTE"));
    }

    @Test
    public void testInscricaoConfirmadaOuEmEspera() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_ins2', 'Palestra Vagas', 'palestra', 'auditorio', 1, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_ins2', 'atv_ins2', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
        }

        // First inscription: should be confirmada (1 vaga, 0 ocupadas)
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_ins2/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp1.statusCode());
        assertTrue(resp1.body().contains("\"status\":\"confirmada\""));

        // Second inscription: should be em_espera (vagas = 1, already 1 ocupada)
        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_ins2/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp2.statusCode());
        assertTrue(resp2.body().contains("\"status\":\"em_espera\""));
        assertTrue(resp2.body().contains("\"posicaoNaEspera\":1"));
    }
}
