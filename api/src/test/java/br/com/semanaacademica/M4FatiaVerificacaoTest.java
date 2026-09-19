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

public class M4FatiaVerificacaoTest {
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
    public void verifica_certificado_sem_autenticacao() throws Exception {
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        try (java.sql.Connection conn = Database.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_publica', 'Atividade verificavel', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO certificados(codigo, atividadeId, participanteId, cargaHorariaMinutos, presencas, encontros, emitidoEm) VALUES('SA26-PUBLICA', 'atv_publica', 'p-carla', 90, 1, 1, '2026-10-20T12:00:00-03:00')");
        }

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/certificados/SA26-PUBLICA"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Map<?, ?> json = mapper.readValue(response.body(), Map.class);
        assertEquals("SA26-PUBLICA", json.get("codigo"));
        assertEquals("Carla Mendes Souza", json.get("participante"));
        assertEquals("Atividade verificavel", json.get("atividade"));
        assertEquals(90, json.get("cargaHorariaMinutos"));
    }

    @Test
    public void certificado_inexistente_retorna_nao_encontrado() throws Exception {
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/certificados/SA26-INEXISTENTE"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertEquals("NAO_ENCONTRADO", mapper.readValue(response.body(), Map.class).get("erro"));
    }
}