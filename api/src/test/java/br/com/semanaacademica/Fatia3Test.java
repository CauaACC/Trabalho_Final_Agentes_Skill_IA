package br.com.semanaacademica;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class Fatia3Test {

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
    public void recusa_edicao_de_salaId_no_patch_com_422_campo_nao_editavel() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        // Create an activity first
        String jsonCreate = "{" +
                "\"titulo\": \"Palestra Original\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 30," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest reqCreate = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCreate))
                .build();

        HttpResponse<String> respCreate = client.send(reqCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respCreate.statusCode());
        
        // Extract id or parse json (or simplified: we know how id is generated or extract from response)
        // Let's parse id or hardcode if we know or extract with simple substring / Jackson
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map map = mapper.readValue(respCreate.body(), java.util.Map.class);
        String atvId = (String) map.get("id");

        // Try PATCH changing salaId (non-editable field)
        String jsonPatch = "{\"salaId\": \"sala-102\"}";

        HttpRequest reqPatch = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonPatch))
                .build();

        HttpResponse<String> respPatch = client.send(reqPatch, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, respPatch.statusCode());
        assertTrue(respPatch.body().contains("CAMPO_NAO_EDITAVEL"));
    }

    @Test
    public void permite_alteracao_de_titulo_e_vagas_no_patch() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        String jsonCreate = "{" +
                "\"titulo\": \"Palestra Vantagens\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 20," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T14:00:00-03:00\", \"fim\": \"2026-10-19T16:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest reqCreate = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCreate))
                .build();

        HttpResponse<String> respCreate = client.send(reqCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respCreate.statusCode());

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map map = mapper.readValue(respCreate.body(), java.util.Map.class);
        String atvId = (String) map.get("id");

        String jsonPatch = "{\"titulo\": \"Palestra Atualizada\", \"vagas\": 35}";

        HttpRequest reqPatch = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonPatch))
                .build();

        HttpResponse<String> respPatch = client.send(reqPatch, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respPatch.statusCode());
        assertTrue(respPatch.body().contains("Palestra Atualizada"));
        assertTrue(respPatch.body().contains("35"));
    }

    @Test
    public void recusa_reducao_de_vagas_abaixo_dos_inscritos_com_409_vagas_abaixo_dos_inscritos() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        String jsonCreate = "{" +
                "\"titulo\": \"Palestra com Inscritos\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 20," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T14:00:00-03:00\", \"fim\": \"2026-10-19T16:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest reqCreate = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCreate))
                .build();

        HttpResponse<String> respCreate = client.send(reqCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respCreate.statusCode());

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map map = mapper.readValue(respCreate.body(), java.util.Map.class);
        String atvId = (String) map.get("id");

        try (java.sql.Connection conn = Database.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("INSERT INTO inscricoes(id, atividadeId, participanteId, status, criadaEm) VALUES(?, ?, ?, ?, ?)")) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(1, "ins_" + i);
                ps.setString(2, atvId);
                ps.setString(3, "p-carla");
                ps.setString(4, "confirmada");
                ps.setString(5, "2026-10-13T10:00:00-03:00");
                ps.executeUpdate();
            }
        }

        String jsonPatch = "{\"vagas\": 3}";

        HttpRequest reqPatch = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonPatch))
                .build();

        HttpResponse<String> respPatch = client.send(reqPatch, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, respPatch.statusCode());
        assertTrue(respPatch.body().contains("VAGAS_ABAIXO_DOS_INSCRITOS"));
    }

    @Test
    public void recusa_cancelamento_de_atividade_que_ja_iniciou_com_422_atividade_ja_iniciada() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        String jsonCreate = "{" +
                "\"titulo\": \"Palestra Iniciada\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 20," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest reqCreate = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCreate))
                .build();

        HttpResponse<String> respCreate = client.send(reqCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respCreate.statusCode());

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map map = mapper.readValue(respCreate.body(), java.util.Map.class);
        String atvId = (String) map.get("id");

        // Advance clock to after start time
        String jsonRelogio = "{\"agora\": \"2026-10-19T10:30:00-03:00\"}";
        HttpRequest reqRelogio = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/relogio"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonRelogio))
                .build();
        client.send(reqRelogio, HttpResponse.BodyHandlers.ofString());

        HttpRequest reqCancel = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId + "/cancelamento"))
                .header("X-Usuario", "org-ana")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> respCancel = client.send(reqCancel, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, respCancel.statusCode());
        assertTrue(respCancel.body().contains("ATIVIDADE_JA_INICIADA"));
    }

    @Test
    public void recusa_cancelamento_de_atividade_ja_cancelada_com_422_atividade_cancelada() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        String jsonCreate = "{" +
                "\"titulo\": \"Palestra Para Cancelar Duas Vezes\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 20," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest reqCreate = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCreate))
                .build();

        HttpResponse<String> respCreate = client.send(reqCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respCreate.statusCode());

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map map = mapper.readValue(respCreate.body(), java.util.Map.class);
        String atvId = (String) map.get("id");

        // First cancel
        HttpRequest reqCancel1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId + "/cancelamento"))
                .header("X-Usuario", "org-ana")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respCancel1 = client.send(reqCancel1, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respCancel1.statusCode());

        // Second cancel
        HttpRequest reqCancel2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId + "/cancelamento"))
                .header("X-Usuario", "org-ana")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> respCancel2 = client.send(reqCancel2, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, respCancel2.statusCode());
        assertTrue(respCancel2.body().contains("ATIVIDADE_CANCELADA"));
    }

    @Test
    public void recusa_edicao_de_atividade_cancelada_com_422_atividade_cancelada() throws Exception {
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/_teste/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(resetReq, HttpResponse.BodyHandlers.ofString());

        String jsonCreate = "{" +
                "\"titulo\": \"Palestra Cancelada Edicao\"," +
                "\"tipo\": \"palestra\"," +
                "\"salaId\": \"sala-101\"," +
                "\"vagas\": 20," +
                "\"encontros\": [" +
                "  {\"inicio\": \"2026-10-19T10:00:00-03:00\", \"fim\": \"2026-10-19T12:00:00-03:00\"}" +
                "]" +
                "}";

        HttpRequest reqCreate = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades"))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCreate))
                .build();

        HttpResponse<String> respCreate = client.send(reqCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, respCreate.statusCode());

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map map = mapper.readValue(respCreate.body(), java.util.Map.class);
        String atvId = (String) map.get("id");

        // Cancel activity
        HttpRequest reqCancel = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId + "/cancelamento"))
                .header("X-Usuario", "org-ana")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        assertEquals(200, client.send(reqCancel, HttpResponse.BodyHandlers.ofString()).statusCode());

        // Try PATCH
        String jsonPatch = "{\"titulo\": \"Novo Titulo\"}";
        HttpRequest reqPatch = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/atividades/" + atvId))
                .header("X-Usuario", "org-ana")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonPatch))
                .build();

        HttpResponse<String> respPatch = client.send(reqPatch, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, respPatch.statusCode());
        assertTrue(respPatch.body().contains("ATIVIDADE_CANCELADA"));
    }
}
