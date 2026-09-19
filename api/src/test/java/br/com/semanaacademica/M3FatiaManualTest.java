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

public class M3FatiaManualTest {
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

    private void prepararEncontro(boolean incluirParticipante) throws Exception {
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
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_manual_m3', 'Palestra manual', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_manual_m3', 'atv_manual_m3', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
            if (incluirParticipante) {
                stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_manual_m3', 'atv_manual_m3', 'p-carla', 'confirmada', NULL, NULL, '2026-10-19T09:00:00-03:00')");
            }
        }
    }

    private HttpResponse<String> registrar(String participanteId, String justificativa) throws Exception {
        return client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_manual_m3/presencas/manual"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"participanteId\":\"" + participanteId + "\",\"justificativa\":\"" + justificativa + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void registra_presenca_manual_com_justificativa() throws Exception {
        prepararEncontro(true);

        HttpResponse<String> response = registrar("p-carla", "Problema no leitor QR");

        assertEquals(201, response.statusCode());
        Map<?, ?> json = mapper.readValue(response.body(), Map.class);
        assertEquals("manual", json.get("origem"));
        assertEquals("Problema no leitor QR", json.get("justificativa"));
    }

    @Test
    public void recusa_presenca_manual_de_nao_inscrito() throws Exception {
        prepararEncontro(false);

        HttpResponse<String> response = registrar("p-carla", "Participante sem inscricao");

        assertEquals(403, response.statusCode());
        assertEquals("NAO_INSCRITO", mapper.readValue(response.body(), Map.class).get("erro"));
    }

    @Test
    public void recusa_segunda_presenca_manual() throws Exception {
        prepararEncontro(true);

        assertEquals(201, registrar("p-carla", "Primeira justificativa").statusCode());
        HttpResponse<String> response = registrar("p-carla", "Segunda justificativa");

        assertEquals(422, response.statusCode());
        assertEquals("LIMITE_DE_MANUAIS", mapper.readValue(response.body(), Map.class).get("erro"));
    }
}