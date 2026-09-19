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

public class M2Fatia4Test {

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
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());
    }

    private void setClock(String agora) throws Exception {
        HttpRequest clockReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"agora\": \"" + agora + "\"}"))
                .build();
        client.send(clockReq, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testVagaLiberadaConvocaPrimeiroDaEspera() throws Exception {
        reset();
        setClock("2026-10-19T08:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_f4_1', 'Atv 1', 'palestra', 'auditorio', 1, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_f4_1', 'atv_f4_1', '2026-10-19T12:00:00-03:00', '2026-10-19T14:00:00-03:00')");
        }

        // Carla gets confirmed
        HttpResponse<String> r1 = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_1/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, r1.statusCode());
        Map m1 = mapper.readValue(r1.body(), Map.class);
        assertEquals("confirmada", m1.get("status"));
        String carlaId = (String) m1.get("id");

        // Diego goes to waiting list
        HttpResponse<String> r2 = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_1/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, r2.statusCode());
        Map m2 = mapper.readValue(r2.body(), Map.class);
        assertEquals("em_espera", m2.get("status"));
        assertEquals(1, m2.get("posicaoNaEspera"));
        String diegoId = (String) m2.get("id");

        // Carla cancels
        HttpResponse<String> rCan = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaId + "/cancelamento"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rCan.statusCode());

        // Check Diego status: should now be convocada
        HttpResponse<String> rDiego = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + diegoId))
                .header("X-Usuario", "p-diego")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rDiego.statusCode());
        Map mDiego = mapper.readValue(rDiego.body(), Map.class);
        assertEquals("convocada", mDiego.get("status"));
        assertNull(mDiego.get("posicaoNaEspera"));
        assertNotNull(mDiego.get("convocadaAte"));
    }

    @Test
    public void testConvocacaoVencidaExpiraECascataProximo() throws Exception {
        reset();
        setClock("2026-10-19T08:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_f4_2', 'Atv 2', 'palestra', 'auditorio', 1, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_f4_2', 'atv_f4_2', '2026-10-19T15:00:00-03:00', '2026-10-19T17:00:00-03:00')");
        }

        // Carla (confirmed), Diego (espera 1), Elisa (espera 2)
        String carlaId = (String) mapper.readValue(client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_2/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString()).body(), Map.class).get("id");

        Map dMap = mapper.readValue(client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_2/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString()).body(), Map.class);
        String diegoId = (String) dMap.get("id");

        Map eMap = mapper.readValue(client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_2/inscricoes"))
                .header("X-Usuario", "p-elisa")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString()).body(), Map.class);
        String elisaId = (String) eMap.get("id");

        // Carla cancels -> Diego gets convocada at 08:00, valid until 10:00 (2 hours)
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + carlaId + "/cancelamento"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());

        // Advance clock past 2 hours (e.g. 10:01:00)
        setClock("2026-10-19T10:01:00-03:00");

        // Trigger check/fetch
        HttpResponse<String> rDiego = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + diegoId))
                .header("X-Usuario", "p-diego")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        Map mDiego = mapper.readValue(rDiego.body(), Map.class);
        assertEquals("expirada", mDiego.get("status"));

        // Elisa should have been automatically convoked in cascade
        HttpResponse<String> rElisa = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/inscricoes/" + elisaId))
                .header("X-Usuario", "p-elisa")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        Map mElisa = mapper.readValue(rElisa.body(), Map.class);
        assertEquals("convocada", mElisa.get("status"));
        assertNotNull(mElisa.get("convocadaAte"));
    }

    @Test
    public void testPosicaoNaEsperaOrdemDeChegada() throws Exception {
        reset();
        setClock("2026-10-19T08:00:00-03:00");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_f4_3', 'Atv 3', 'palestra', 'auditorio', 1, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_f4_3', 'atv_f4_3', '2026-10-19T15:00:00-03:00', '2026-10-19T17:00:00-03:00')");
        }

        // Carla (confirmed)
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_3/inscricoes"))
                .header("X-Usuario", "p-carla")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());

        // Diego (espera 1)
        Map d = mapper.readValue(client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_3/inscricoes"))
                .header("X-Usuario", "p-diego")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString()).body(), Map.class);
        assertEquals(1, d.get("posicaoNaEspera"));

        // Elisa (espera 2)
        Map e = mapper.readValue(client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_3/inscricoes"))
                .header("X-Usuario", "p-elisa")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString()).body(), Map.class);
        assertEquals(2, e.get("posicaoNaEspera"));

        // Fabio (espera 3)
        Map f = mapper.readValue(client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_f4_3/inscricoes"))
                .header("X-Usuario", "p-fabio")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString()).body(), Map.class);
        assertEquals(3, f.get("posicaoNaEspera"));
    }
}
