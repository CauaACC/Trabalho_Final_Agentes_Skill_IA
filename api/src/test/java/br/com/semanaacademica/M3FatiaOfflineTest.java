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

public class M3FatiaOfflineTest {
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
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_qr_offline', 'Palestra QR offline', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_qr_offline', 'atv_qr_offline', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES('ins_qr_offline', 'atv_qr_offline', 'p-carla', 'confirmada', NULL, NULL, '2026-10-19T09:00:00-03:00')");
        }
    }

    private String obterCodigo() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_qr_offline/codigo"))
                .header("X-Usuario", "org-ana")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        return (String) mapper.readValue(response.body(), Map.class).get("codigo");
    }

    private HttpResponse<String> registrar(String codigo, String lidoEm) throws Exception {
        return client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/encontros/enc_qr_offline/presencas"))
                .header("X-Usuario", "p-carla")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"codigo\":\"" + codigo + "\",\"lidoEm\":\"" + lidoEm + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void aceita_leitura_offline_dentro_da_janela() throws Exception {
        prepararEncontro();

        HttpResponse<String> response = registrar(obterCodigo(), "2026-10-19T10:10:00-03:00");

        assertEquals(201, response.statusCode());
        assertEquals("qr_offline", mapper.readValue(response.body(), Map.class).get("origem"));
    }

    @Test
    public void recusa_sincronizacao_tardia() throws Exception {
        prepararEncontro();

        HttpResponse<String> response = registrar(obterCodigo(), "2026-10-19T13:30:00-03:00");

        assertEquals(422, response.statusCode());
        assertEquals("SINCRONIZACAO_TARDIA", mapper.readValue(response.body(), Map.class).get("erro"));
    }
}