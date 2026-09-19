package br.com.semanaacademica;

import com.fasterxml.jackson.databind.ObjectMapper;
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

public class M2Fatia5Test {

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
    public void confirmacao_convocacao_com_conflito_de_horario_retorna_409_e_mantem_convocada() throws Exception {
        reset();
        setClock("2026-10-19T08:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_a', 'Atv A', 'palestra', 'auditorio', 1, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_a', 'atv_a', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");

            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_b', 'Atv B', 'palestra', 'auditorio', 1, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_b', 'atv_b', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
        }

        // Diego takes Atv A (confirmed)
        HttpResponse<String> respDiegoA = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_a/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        String diegoInscId = (String) mapper.readValue(respDiegoA.body(), Map.class).get("id");

        // Carla takes Atv A (waitlist pos 1) -> get carlaInscAtvAId
        HttpResponse<String> respCarlaA = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_a/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        String carlaInscAtvAId = (String) mapper.readValue(respCarlaA.body(), Map.class).get("id");

        // Carla also takes Atv B (confirmed)
        HttpResponse<String> respB = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_b/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respB.statusCode());

        // Diego cancels Atv A -> Carla gets convoked for Atv A
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + diegoInscId + "/cancelamento"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());

        // Verify Carla's inscription for Atv A is now convocada
        HttpResponse<String> respCarlaAGet = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaInscAtvAId))
                .header("X-Usuario", "p-carla")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("convocada", mapper.readValue(respCarlaAGet.body(), Map.class).get("status"));

        // Carla tries to confirm Atv A, but she has Atv B confirmed (overlapping). Should return 409 CONFLITO_DE_HORARIO
        HttpResponse<String> respConf = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaInscAtvAId + "/confirmacao"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(409, respConf.statusCode());
        assertTrue(respConf.body().contains("CONFLITO_DE_HORARIO"));

        // Check that inscription is still 'convocada'
        HttpResponse<String> respGet = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaInscAtvAId))
                .header("X-Usuario", "p-carla")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("convocada", mapper.readValue(respGet.body(), Map.class).get("status"));
    }

    @Test
    public void confirmacao_convocacao_com_limite_de_minicursos_retorna_422_e_mantem_convocada() throws Exception {
        reset();
        setClock("2026-10-19T08:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            for (int i = 1; i <= 4; i++) {
                int hour = 8 + i * 2;
                stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_m" + i + "', 'Minicurso " + i + "', 'minicurso', 'lab-3', 1, 0)");
                stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_m" + i + "', 'atv_m" + i + "', '2026-10-19T" + hour + ":00:00-03:00', '2026-10-19T" + (hour+1) + ":00:00-03:00')");
            }
        }

        // Carla takes M1, M2, M3 (confirmed)
        for (int i = 1; i <= 3; i++) {
            HttpResponse<String> r = client.send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/atividades/atv_m" + i + "/inscricoes"))
                    .header("X-Usuario", "p-carla")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(201, r.statusCode());
        }

        // Diego takes M4 (confirmed)
        HttpResponse<String> respDiegoM4 = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_m4/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        String diegoM4Id = (String) mapper.readValue(respDiegoM4.body(), Map.class).get("id");

        // Carla takes M4 (waitlist pos 1)
        HttpResponse<String> respCarlaM4 = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_m4/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        String carlaM4Id = (String) mapper.readValue(respCarlaM4.body(), Map.class).get("id");

        // Diego cancels M4 -> Carla convoked for M4
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + diegoM4Id + "/cancelamento"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());

        // Verify Carla's inscription for M4 is convocada
        HttpResponse<String> respCarlaM4Get = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaM4Id))
                .header("X-Usuario", "p-carla")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("convocada", mapper.readValue(respCarlaM4Get.body(), Map.class).get("status"));

        // Carla tries to confirm M4, but she already has 3 minicursos confirmed. Should return 422 LIMITE_DE_MINICURSOS
        HttpResponse<String> respConf = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaM4Id + "/confirmacao"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(422, respConf.statusCode());
        assertTrue(respConf.body().contains("LIMITE_DE_MINICURSOS"));

        // Check that inscription is still 'convocada'
        HttpResponse<String> respGet = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaM4Id))
                .header("X-Usuario", "p-carla")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("convocada", mapper.readValue(respGet.body(), Map.class).get("status"));
    }

    @Test
    public void confirmacao_com_prazo_vencido_retorna_422_convocacao_expirada() throws Exception {
        reset();
        setClock("2026-10-19T08:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_exp', 'Atv Exp', 'palestra', 'auditorio', 1, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_exp', 'atv_exp', '2026-10-19T15:00:00-03:00', '2026-10-19T17:00:00-03:00')");
        }

        // Diego confirmed
        HttpResponse<String> respDiego = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_exp/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        String diegoId = (String) mapper.readValue(respDiego.body(), Map.class).get("id");

        // Carla waitlist
        HttpResponse<String> respCarla = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_exp/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        String carlaId = (String) mapper.readValue(respCarla.body(), Map.class).get("id");

        // Diego cancels -> Carla convoked at 08:00, valid until 10:00
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + diegoId + "/cancelamento"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());

        // Advance clock past 2 hours (10:01:00)
        setClock("2026-10-19T10:01:00-03:00");

        HttpResponse<String> respConf = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaId + "/confirmacao"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(422, respConf.statusCode());
        assertTrue(respConf.body().contains("CONVOCACAO_EXPIRADA"));
    }

    @Test
    public void confirmacao_inscricao_sem_convocacao_retorna_422() throws Exception {
        reset();
        setClock("2026-10-19T08:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_sc', 'Atv SC', 'palestra', 'auditorio', 1, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_sc', 'atv_sc', '2026-10-19T15:00:00-03:00', '2026-10-19T17:00:00-03:00')");
        }

        String insId = (String) mapper.readValue(client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_sc/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString()).body(), Map.class).get("id");

        HttpResponse<String> resp = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + insId + "/confirmacao"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("SEM_CONVOCACAO"));
    }
}
