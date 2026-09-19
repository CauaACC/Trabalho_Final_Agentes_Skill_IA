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

public class M4FatiaRegrasTest {
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
        if (app != null) app.stop();
    }

    private void resetAndClock(String agora) throws Exception {
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"agora\":\"" + agora + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private void criarAtividade(String situacao, boolean inscrito, int encontros) throws Exception {
        try (java.sql.Connection conn = Database.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_regras', 'Minicurso regras', 'minicurso', 'lab-3', 20, " + ("cancelada".equals(situacao) ? 1 : 0) + ")");
            for (int indice = 1; indice <= encontros; indice++) {
                int hora = 10 + ((indice - 1) * 2);
                stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_regras_" + indice + "', 'atv_regras', '2026-10-19T" + hora + ":00:00-03:00', '2026-10-19T" + (hora + 1) + ":00:00-03:00')");
            }
            if (inscrito) {
                stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, criadaEm) VALUES('ins_regras', 'atv_regras', 'p-carla', 'confirmada', '2026-10-18T09:00:00-03:00')");
            }
        }
    }

    private HttpResponse<String> emitir() throws Exception {
        return client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades/atv_regras/certificado"))
                .header("X-Usuario", "p-carla").POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void recusa_atividade_nao_encerrada() throws Exception {
        resetAndClock("2026-10-19T10:30:00-03:00");
        criarAtividade("prevista", true, 1);

        HttpResponse<String> response = emitir();

        assertEquals(422, response.statusCode());
        assertEquals("ATIVIDADE_NAO_ENCERRADA", mapper.readValue(response.body(), Map.class).get("erro"));
    }

    @Test
    public void recusa_participante_nao_inscrito() throws Exception {
        resetAndClock("2026-10-19T13:00:00-03:00");
        criarAtividade("prevista", false, 1);

        HttpResponse<String> response = emitir();

        assertEquals(403, response.statusCode());
        assertEquals("NAO_INSCRITO", mapper.readValue(response.body(), Map.class).get("erro"));
    }

    @Test
    public void recusa_atividade_cancelada() throws Exception {
        resetAndClock("2026-10-19T13:00:00-03:00");
        criarAtividade("cancelada", true, 1);

        HttpResponse<String> response = emitir();

        assertEquals(422, response.statusCode());
        assertEquals("ATIVIDADE_CANCELADA", mapper.readValue(response.body(), Map.class).get("erro"));
    }

    @Test
    public void recusa_presenca_insuficiente() throws Exception {
        resetAndClock("2026-10-19T13:00:00-03:00");
        criarAtividade("prevista", true, 2);
        try (java.sql.Connection conn = Database.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO presencas(id, encontroId, participanteId, origem, lidoEm, registradaEm, justificativa) VALUES('pre_regras', 'enc_regras_1', 'p-carla', 'qr', '2026-10-19T10:10:00-03:00', '2026-10-19T10:10:00-03:00', NULL)");
        }

        HttpResponse<String> response = emitir();

        assertEquals(422, response.statusCode());
        assertEquals("PRESENCA_INSUFICIENTE", mapper.readValue(response.body(), Map.class).get("erro"));
    }
}