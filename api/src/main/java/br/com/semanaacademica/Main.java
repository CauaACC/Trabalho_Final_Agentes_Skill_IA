package br.com.semanaacademica;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.sql.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

public class Main {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        int port = 3000;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            port = Integer.parseInt(portEnv);
        }
        startApp(port);
    }

    public static Javalin startApp(int port) {
        Database.initDb();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
        });

        app.before(ctx -> {
            String path = ctx.path();
            if (path.startsWith("/_teste/") || path.matches("^/certificados/[^/]+$")) {
                return;
            }

            String xUsuario = ctx.header("X-Usuario");
            if (xUsuario == null || !Database.userExists(xUsuario)) {
                ctx.status(401);
                Map<String, String> err = new HashMap<>();
                err.put("erro", "USUARIO_DESCONHECIDO");
                err.put("mensagem", "Usuário não informado ou desconhecido");
                ctx.json(err);
                ctx.skipRemainingHandlers();
                return;
            }
        });

        app.post("/_teste/reset", ctx -> {
            if (!isTestMode()) {
                ctx.status(404);
                return;
            }
            Database.resetInitialData();
            ctx.status(204);
        });

        app.put("/_teste/relogio", ctx -> {
            if (!isTestMode()) {
                ctx.status(404);
                return;
            }
            Map body = ctx.bodyAsClass(Map.class);
            String agoraStr = (String) body.get("agora");
            OffsetDateTime dt = OffsetDateTime.parse(agoraStr);
            Database.setClock(dt);
            ctx.json(Map.of("agora", dt.toString()));
        });

        app.get("/_teste/relogio", ctx -> {
            if (!isTestMode()) {
                ctx.status(404);
                return;
            }
            ctx.json(Map.of("agora", Database.getClock().toString()));
        });

        app.get("/salas", ctx -> {
            List<Map<String, Object>> salas = new ArrayList<>();
            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, nome, capacidade FROM salas")) {
                while (rs.next()) {
                    Map<String, Object> sala = new HashMap<>();
                    sala.put("id", rs.getString("id"));
                    sala.put("nome", rs.getString("nome"));
                    sala.put("capacidade", rs.getInt("capacidade"));
                    salas.add(sala);
                }
            }
            ctx.json(salas);
        });

        app.get("/atividades", ctx -> {
            String dia = ctx.queryParam("dia");
            String tipo = ctx.queryParam("tipo");

            List<Map<String, Object>> atividades = new ArrayList<>();
            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, titulo, tipo, salaId, vagas, cancelada FROM atividades")) {
                while (rs.next()) {
                    String atvId = rs.getString("id");
                    Map<String, Object> atv = buildAtividade(conn, atvId);
                    if (atv == null) continue;

                    boolean match = true;
                    if (tipo != null && !tipo.isEmpty()) {
                        if (!tipo.equalsIgnoreCase((String) atv.get("tipo"))) {
                            match = false;
                        }
                    }
                    if (dia != null && !dia.isEmpty()) {
                        boolean matchDia = false;
                        List<Map<String, String>> encontros = (List<Map<String, String>>) atv.get("encontros");
                        for (Map<String, String> enc : encontros) {
                            String inicio = enc.get("inicio");
                            if (inicio.startsWith(dia)) {
                                matchDia = true;
                                break;
                            }
                        }
                        if (!matchDia) match = false;
                    }

                    if (match) {
                        atividades.add(atv);
                    }
                }
            }

            atividades.sort((a, b) -> {
                List<Map<String, String>> encA = (List<Map<String, String>>) a.get("encontros");
                List<Map<String, String>> encB = (List<Map<String, String>>) b.get("encontros");
                String startA = encA.isEmpty() ? "" : encA.get(0).get("inicio");
                String startB = encB.isEmpty() ? "" : encB.get(0).get("inicio");
                int cmp = startA.compareTo(startB);
                if (cmp != 0) return cmp;
                return ((String) a.get("titulo")).compareTo((String) b.get("titulo"));
            });

            ctx.json(atividades);
        });

        app.get("/atividades/{id}", ctx -> {
            String id = ctx.pathParam("id");
            try (Connection conn = Database.getConnection()) {
                Map<String, Object> atv = buildAtividade(conn, id);
                if (atv == null) {
                    ctx.status(404);
                    ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Atividade não encontrada"));
                    return;
                }
                ctx.json(atv);
            }
        });

        app.start(port);
        return app;
    }

    private static boolean isTestMode() {
        return "1".equals(System.getenv("MODO_TESTE")) || "1".equals(System.getProperty("MODO_TESTE"));
    }

    public static Map<String, Object> buildAtividade(Connection conn, String atvId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id, titulo, tipo, salaId, vagas, cancelada FROM atividades WHERE id = ?");
        ps.setString(1, atvId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            return null;
        }

        Map<String, Object> atv = new HashMap<>();
        atv.put("id", rs.getString("id"));
        atv.put("titulo", rs.getString("titulo"));
        atv.put("tipo", rs.getString("tipo"));
        atv.put("salaId", rs.getString("salaId"));
        atv.put("vagas", rs.getInt("vagas"));
        boolean isCancelada = rs.getInt("cancelada") == 1;

        List<Map<String, String>> encontros = new ArrayList<>();
        PreparedStatement psEnc = conn.prepareStatement("SELECT id, inicio, fim FROM encontros WHERE atividadeId = ? ORDER BY inicio ASC");
        psEnc.setString(1, atvId);
        ResultSet rsEnc = psEnc.executeQuery();
        long cargaHorariaMinutos = 0;
        OffsetDateTime primeiroInicio = null;
        OffsetDateTime ultimoFim = null;

        while (rsEnc.next()) {
            Map<String, String> enc = new HashMap<>();
            enc.put("id", rsEnc.getString("id"));
            String inicioStr = rsEnc.getString("inicio");
            String fimStr = rsEnc.getString("fim");
            enc.put("inicio", inicioStr);
            enc.put("fim", fimStr);
            encontros.add(enc);

            OffsetDateTime dtInicio = OffsetDateTime.parse(inicioStr);
            OffsetDateTime dtFim = OffsetDateTime.parse(fimStr);

            if (primeiroInicio == null || dtInicio.isBefore(primeiroInicio)) {
                primeiroInicio = dtInicio;
            }
            if (ultimoFim == null || dtFim.isAfter(ultimoFim)) {
                ultimoFim = dtFim;
            }

            cargaHorariaMinutos += Duration.between(dtInicio, dtFim).toMinutes();
        }

        atv.put("encontros", encontros);
        atv.put("cargaHorariaMinutos", cargaHorariaMinutos);

        String situacao = "prevista";
        if (isCancelada) {
            situacao = "cancelada";
        } else if (primeiroInicio != null && ultimoFim != null) {
            OffsetDateTime agora = Database.getClock();
            if (agora.isBefore(primeiroInicio)) {
                situacao = "prevista";
            } else if (!agora.isAfter(ultimoFim)) {
                situacao = "em_andamento";
            } else {
                situacao = "encerrada";
            }
        }
        atv.put("situacao", situacao);

        int ocupadas = 0;
        int emEspera = 0;
        try {
            PreparedStatement psIns = conn.prepareStatement("SELECT status, count(*) FROM inscricoes WHERE atividadeId = ? GROUP BY status");
            psIns.setString(1, atvId);
            ResultSet rsIns = psIns.executeQuery();
            while (rsIns.next()) {
                String st = rsIns.getString(1);
                int count = rsIns.getInt(2);
                if ("confirmada".equals(st) || "convocada".equals(st)) {
                    ocupadas += count;
                } else if ("em_espera".equals(st)) {
                    emEspera += count;
                }
            }
        } catch (SQLException ignored) {}

        atv.put("ocupadas", ocupadas);
        atv.put("vagasRestantes", Math.max(0, rs.getInt("vagas") - ocupadas));
        atv.put("emEspera", emEspera);

        return atv;
    }
}
