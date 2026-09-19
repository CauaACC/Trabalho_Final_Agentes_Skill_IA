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

import static org.junit.jupiter.api.Assertions.*;

public class M3FatiaBaseTest {
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

    private void reset() throws Exception {
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private void setClock(String agora) throws Exception {
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"agora\": \"" + agora + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void gera_codigo_do_encontro() throws Exception {
        reset();
        setClock("2026-10-19T09:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_m3', 'Minicurso M3', 'minicurso', 'lab-3', 20, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_m3', 'atv_m3', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_m3_1', 'atv_m3', 'p-carla', 'confirmada', NULL, NULL, '2026-10-19T09:00:00-03:00')");
        }

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_m3/codigo"))
                .header("X-Usuario", "org-ana")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Map<?, ?> json = mapper.readValue(response.body(), Map.class);
        assertEquals("enc_m3", json.get("encontroId"));
        assertNotNull(json.get("codigo"));
        assertEquals(6, ((String) json.get("codigo")).length());
        assertNotNull(json.get("trocaEm"));
        assertNotNull(json.get("validoAte"));
    }

    @Test
    public void registra_presenca_por_qr_valida() throws Exception {
        reset();
        setClock("2026-10-19T09:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_qr', 'Palestra QR', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_qr', 'atv_qr', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_qr_1', 'atv_qr', 'p-carla', 'confirmada', NULL, NULL, '2026-10-19T09:00:00-03:00')");
        }

        HttpResponse<String> codigoResp = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_qr/codigo"))
                .header("X-Usuario", "org-ana")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        String codigo = (String) mapper.readValue(codigoResp.body(), Map.class).get("codigo");

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_qr/presencas"))
                .header("X-Usuario", "p-carla")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"codigo\":\"" + codigo + "\",\"lidoEm\":\"2026-10-19T10:05:00-03:00\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Map<?, ?> json = mapper.readValue(response.body(), Map.class);
        assertEquals("qr_offline", json.get("origem"));
        assertEquals("p-carla", json.get("participanteId"));
        assertEquals("enc_qr", json.get("encontroId"));
    }

    @Test
    public void recusa_codigo_invalido() throws Exception {
        reset();
        setClock("2026-10-19T09:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_bad', 'Atividade Bad', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_bad', 'atv_bad', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_bad_1', 'atv_bad', 'p-carla', 'confirmada', NULL, NULL, '2026-10-19T09:00:00-03:00')");
        }

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_bad/presencas"))
                .header("X-Usuario", "p-carla")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"codigo\":\"ZZZZZZ\",\"lidoEm\":\"2026-10-19T10:05:00-03:00\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("CODIGO_INVALIDO"));
    }

    @Test
    public void recusa_presenca_manual_sem_justificativa() throws Exception {
        reset();
        setClock("2026-10-19T09:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_man', 'Manual', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_man', 'atv_man', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_man_1', 'atv_man', 'p-carla', 'confirmada', NULL, NULL, '2026-10-19T09:00:00-03:00')");
        }

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_man/presencas/manual"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"participanteId\":\"p-carla\",\"justificativa\":\"\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("JUSTIFICATIVA_OBRIGATORIA"));
    }
}
