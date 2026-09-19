package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class M2Fatia2Test {

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
    public void testConflitoDeHorario() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_c1', 'Atividade 1', 'palestra', 'auditorio', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_c1', 'atv_c1', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");

            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_c2', 'Atividade 2', 'palestra', 'sala-101', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_c2', 'atv_c2', '2026-10-19T11:00:00-03:00', '2026-10-19T13:00:00-03:00')");
        }

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_c1/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        assertEquals(201, client.send(req1, HttpResponse.BodyHandlers.ofString()).statusCode());

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_c2/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, resp2.statusCode());
        assertTrue(resp2.body().contains("CONFLITO_DE_HORARIO"));
    }

    @Test
    public void testEncostarHorariosNaoEConflito() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_e1', 'Atividade 1', 'palestra', 'auditorio', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_e1', 'atv_e1', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");

            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_e2', 'Atividade 2', 'palestra', 'sala-101', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_e2', 'atv_e2', '2026-10-19T12:00:00-03:00', '2026-10-19T14:00:00-03:00')");
        }

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_e1/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        assertEquals(201, client.send(req1, HttpResponse.BodyHandlers.ofString()).statusCode());

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_e2/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        assertEquals(201, client.send(req2, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    public void testEsperaNaoGeraConflito() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            // atv_es1 has 0 vagas (so p-elisa goes to espera)
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_es1', 'Atividade Espera', 'palestra', 'auditorio', 0, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_es1', 'atv_es1', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");

            // atv_es2 has 10 vagas, same time
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_es2', 'Atividade Vaga', 'palestra', 'sala-101', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_es2', 'atv_es2', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
        }

        // Register in atv_es1 -> em_espera
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_es1/inscricoes"))
                .header("X-Usuario", "p-elisa")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp1.statusCode());
        assertTrue(resp1.body().contains("em_espera"));

        // Register in atv_es2 (overlapping time, but since first is em_espera, should succeed with confirmada)
        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_es2/inscricoes"))
                .header("X-Usuario", "p-elisa")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp2.statusCode());
        assertTrue(resp2.body().contains("confirmada"));
    }

    @Test
    public void testLimiteDeMinicursos() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            for (int i = 1; i <= 4; i++) {
                stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_m" + i + "', 'Minicurso " + i + "', 'minicurso', 'lab-3', 10, 0)");
                stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_m" + i + "_1', 'atv_m" + i + "', '2026-10-19T0" + (i + 1) + ":00:00-03:00', '2026-10-19T0" + (i + 2) + ":00:00-03:00')");
                stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_m" + i + "_2', 'atv_m" + i + "', '2026-10-20T0" + (i + 1) + ":00:00-03:00', '2026-10-20T0" + (i + 2) + ":00:00-03:00')");
            }
        }

        // Register in 3 minicursos
        for (int i = 1; i <= 3; i++) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/atividades/atv_m" + i + "/inscricoes"))
                    .header("X-Usuario", "p-elisa")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            assertEquals(201, client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode());
        }

        // Try to register in 4th minicurso
        HttpRequest req4 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_m4/inscricoes"))
                .header("X-Usuario", "p-elisa")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp4 = client.send(req4, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, resp4.statusCode());
        assertTrue(resp4.body().contains("LIMITE_DE_MINICURSOS"));
    }

    @Test
    public void testPalestrasNaoContamParaLimite() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            for (int i = 1; i <= 3; i++) {
                stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_mp" + i + "', 'Minicurso " + i + "', 'minicurso', 'lab-3', 10, 0)");
                stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_mp" + i + "_1', 'atv_mp" + i + "', '2026-10-19T0" + (i + 1) + ":00:00-03:00', '2026-10-19T0" + (i + 2) + ":00:00-03:00')");
                stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_mp" + i + "_2', 'atv_mp" + i + "', '2026-10-20T0" + (i + 1) + ":00:00-03:00', '2026-10-20T0" + (i + 2) + ":00:00-03:00')");
            }
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_plt', 'Palestra Extra', 'palestra', 'auditorio', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_plt', 'atv_plt', '2026-10-21T10:00:00-03:00', '2026-10-21T12:00:00-03:00')");
        }

        // Register in 3 minicursos
        for (int i = 1; i <= 3; i++) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/atividades/atv_mp" + i + "/inscricoes"))
                    .header("X-Usuario", "p-gabriela")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            assertEquals(201, client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode());
        }

        // Register in palestra (should succeed because palestras don't count for minicursos limit)
        HttpRequest reqPlt = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_plt/inscricoes"))
                .header("X-Usuario", "p-gabriela")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        assertEquals(201, client.send(reqPlt, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    public void testOrdemDePrecedencia() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_p1', 'Atividade P1', 'palestra', 'auditorio', 10, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_p1', 'atv_p1', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
        }

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_p1/inscricoes"))
                .header("X-Usuario", "p-fabio")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        assertEquals(201, client.send(req1, HttpResponse.BodyHandlers.ofString()).statusCode());

        HttpRequest reqAgain = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_p1/inscricoes"))
                .header("X-Usuario", "p-fabio")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respAgain = client.send(reqAgain, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, respAgain.statusCode());
        assertTrue(respAgain.body().contains("JA_INSCRITO"));
    }
}
