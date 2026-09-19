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
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class M4FatiaEmissaoTest {
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
    public void emite_certificado_e_retorna_o_mesmo_na_repeticao() throws Exception {
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"agora\":\"2026-10-20T12:00:00-03:00\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());

        try (java.sql.Connection conn = Database.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_cert', 'Minicurso certificado', 'minicurso', 'lab-3', 20, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_cert', 'atv_cert', '2026-10-19T10:00:00-03:00', '2026-10-19T12:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, criadaEm) VALUES('ins_cert', 'atv_cert', 'p-carla', 'confirmada', '2026-10-19T09:00:00-03:00')");
        }

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/atividades/atv_cert/certificado"))
                .header("X-Usuario", "p-carla").POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> first = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> second = client.send(request, HttpResponse.BodyHandlers.ofString());
        Map<?, ?> firstJson = mapper.readValue(first.body(), Map.class);
        Map<?, ?> secondJson = mapper.readValue(second.body(), Map.class);

        assertEquals(201, first.statusCode());
        assertEquals(200, second.statusCode());
        assertNotNull(firstJson.get("codigo"));
        assertEquals(firstJson.get("codigo"), secondJson.get("codigo"));
        assertEquals(120, firstJson.get("cargaHorariaMinutos"));
    }
}