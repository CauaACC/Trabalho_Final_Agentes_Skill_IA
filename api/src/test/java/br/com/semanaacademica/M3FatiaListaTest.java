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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class M3FatiaListaTest {
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

    private void prepararEncontro() throws Exception {
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
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_lista_m3', 'Palestra lista', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_lista_m3', 'atv_lista_m3', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_lista_1', 'atv_lista_m3', 'p-carla', 'confirmada', NULL, NULL, '2026-10-19T09:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_lista_2', 'atv_lista_m3', 'p-heitor', 'confirmada', NULL, NULL, '2026-10-19T09:01:00-03:00')");
        }
    }

    private HttpResponse<String> registrarManual(String participanteId) throws Exception {
        return client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_lista_m3/presencas/manual"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"participanteId\":\"" + participanteId + "\",\"justificativa\":\"Registro de teste\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void organizacao_consulta_presencas_em_ordem() throws Exception {
        prepararEncontro();
        assertEquals(201, registrarManual("p-carla").statusCode());
        assertEquals(201, registrarManual("p-heitor").statusCode());

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_lista_m3/presencas"))
                .header("X-Usuario", "org-ana")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        List<?> presencas = mapper.readValue(response.body(), List.class);
        assertEquals(2, presencas.size());
        Map<?, ?> primeira = (Map<?, ?>) presencas.get(0);
        assertEquals("p-carla", primeira.get("participanteId"));
        assertEquals("manual", primeira.get("origem"));
        assertTrue(primeira.containsKey("id"));
        assertTrue(primeira.containsKey("encontroId"));
        assertTrue(primeira.containsKey("lidoEm"));
        assertTrue(primeira.containsKey("registradaEm"));
        assertTrue(primeira.containsKey("justificativa"));
    }

    @Test
    public void participante_nao_consulta_lista_de_presencas() throws Exception {
        prepararEncontro();

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_lista_m3/presencas"))
                .header("X-Usuario", "p-carla")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
        assertEquals("SOMENTE_ORGANIZACAO", mapper.readValue(response.body(), Map.class).get("erro"));
    }
}