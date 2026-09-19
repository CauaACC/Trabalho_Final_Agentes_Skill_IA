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

public class M4FatiaListaTest {
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

    @Test
    public void lista_apenas_certificados_do_participante() throws Exception {
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        try (java.sql.Connection conn = Database.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO certificados(codigo, atividadeId, participanteId, cargaHorariaMinutos, presencas, encontros, emitidoEm) VALUES('SA26-CARLA01', 'atv_um', 'p-carla', 120, 1, 1, '2026-10-20T12:00:00-03:00')");
            stmt.execute("INSERT INTO certificados(codigo, atividadeId, participanteId, cargaHorariaMinutos, presencas, encontros, emitidoEm) VALUES('SA26-HEITOR1', 'atv_dois', 'p-heitor', 240, 2, 2, '2026-10-20T13:00:00-03:00')");
        }

        HttpResponse<String> response = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/certificados"))
                .header("X-Usuario", "p-carla").GET().build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        List<?> certificados = mapper.readValue(response.body(), List.class);
        assertEquals(1, certificados.size());
        assertEquals("SA26-CARLA01", ((Map<?, ?>) certificados.get(0)).get("codigo"));
    }

    @Test
    public void organizacao_nao_lista_certificados_de_participante() throws Exception {
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> response = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/certificados"))
                .header("X-Usuario", "org-ana").GET().build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
        assertEquals("SOMENTE_PARTICIPANTE", mapper.readValue(response.body(), Map.class).get("erro"));
    }
}