package br.com.semanaacademica;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class M3FatiaQrOnlineTest {
    private static io.javalin.Javalin app;
    private static String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

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
    public void registra_presenca_online_sem_lido_em() throws Exception {
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"agora\": \"2026-10-19T10:05:00-03:00\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_qr_online', 'Palestra QR online', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_qr_online', 'atv_qr_online', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_qr_online', 'atv_qr_online', 'p-carla', 'confirmada', NULL, NULL, '2026-10-19T09:00:00-03:00')");
        }

        HttpResponse<String> codigoResp = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_qr_online/codigo"))
                .header("X-Usuario", "org-ana")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        String codigo = (String) mapper.readValue(codigoResp.body(), Map.class).get("codigo");

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_qr_online/presencas"))
                .header("X-Usuario", "p-carla")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"codigo\":\"" + codigo + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertEquals("qr", mapper.readValue(response.body(), Map.class).get("origem"));
    }
}