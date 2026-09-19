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

public class M4FatiaExtratoTest {
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
    public void gera_extrato_com_totais_por_tipo() throws Exception {
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        try (java.sql.Connection conn = Database.getConnection(); java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_ext_palestra', 'Palestra extrato', 'palestra', 'auditorio', 200, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_ext_palestra', 'atv_ext_palestra', '2026-10-19T10:00:00-03:00', '2026-10-19T11:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, criadaEm) VALUES('ins_ext_palestra', 'atv_ext_palestra', 'p-carla', 'confirmada', '2026-10-18T09:00:00-03:00')");
            stmt.execute("INSERT INTO certificados(codigo, atividadeId, participanteId, cargaHorariaMinutos, presencas, encontros, emitidoEm) VALUES('SA26-EXT001', 'atv_ext_palestra', 'p-carla', 60, 1, 1, '2026-10-20T12:00:00-03:00')");
            stmt.execute("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES('atv_ext_mini', 'Minicurso extrato', 'minicurso', 'lab-3', 20, 0)");
            stmt.execute("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES('enc_ext_mini', 'atv_ext_mini', '2026-10-19T14:00:00-03:00', '2026-10-19T16:00:00-03:00')");
            stmt.execute("INSERT INTO inscricoes(id, atividadeId, participanteId, status, criadaEm) VALUES('ins_ext_mini', 'atv_ext_mini', 'p-carla', 'confirmada', '2026-10-18T09:00:00-03:00')");
        }

        HttpResponse<String> response = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/extrato"))
                .header("X-Usuario", "p-carla").GET().build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Map<?, ?> extrato = mapper.readValue(response.body(), Map.class);
        List<?> itens = (List<?>) extrato.get("itens");
        assertEquals(2, itens.size());
        assertEquals(60, extrato.get("palestrasMinutos"));
        assertEquals(120, extrato.get("minicursosMinutos"));
        assertEquals(180, extrato.get("totalMinutos"));
        assertEquals(180, extrato.get("aproveitadoMinutos"));
        assertEquals("SA26-EXT001", ((Map<?, ?>) itens.get(1)).get("codigo"));
    }

    @Test
    public void organizacao_nao_consulta_extrato() throws Exception {
        client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> response = client.send(HttpRequest.newBuilder().uri(URI.create(baseUrl + "/extrato"))
                .header("X-Usuario", "org-ana").GET().build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
        assertEquals("SOMENTE_PARTICIPANTE", mapper.readValue(response.body(), Map.class).get("erro"));
    }
}