package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class Fatia1Test {

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
    public void testGetSalasUnauthorizedWithoutHeader() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/salas"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("USUARIO_DESCONHECIDO"));
    }

    @Test
    public void testGetSalasSuccessWithHeader() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/salas"))
                .header("X-Usuario", "p-carla")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Auditório Central"));
        assertTrue(response.body().contains("Sala 101"));
    }

    @Test
    public void testGetAtividadesAndDetail() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_test1', 'Palestra Java', 'palestra', 'auditorio', 100, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_test1', 'atv_test1', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "p-carla")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Palestra Java"));
        assertTrue(response.body().contains("120"));

        HttpRequest detailReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/atv_test1"))
                .header("X-Usuario", "p-carla")
                .GET()
                .build();

        HttpResponse<String> detailResp = client.send(detailReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, detailResp.statusCode());
        assertTrue(detailResp.body().contains("prevista"));
    }
}
