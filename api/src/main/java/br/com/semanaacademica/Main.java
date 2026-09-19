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

        app.post("/atividades", ctx -> {
            String xUsuario = ctx.header("X-Usuario");
            String role = Database.getUserRole(xUsuario);
            if (!"organizacao".equals(role)) {
                ctx.status(403);
                ctx.json(Map.of("erro", "SOMENTE_ORGANIZACAO", "mensagem", "Apenas organização"));
                return;
            }

            Map body;
            try {
                body = ctx.bodyAsClass(Map.class);
            } catch (Exception e) {
                ctx.status(422);
                ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Corpo inválido"));
                return;
            }

            if (body == null || !body.containsKey("titulo") || !body.containsKey("tipo") || !body.containsKey("salaId") || !body.containsKey("vagas") || !body.containsKey("encontros")) {
                ctx.status(422);
                ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Campos obrigatórios ausentes"));
                return;
            }

            String titulo = (String) body.get("titulo");
            String tipo = (String) body.get("tipo");
            String salaId = (String) body.get("salaId");
            Object vagasObj = body.get("vagas");
            List<Map<String, String>> encontrosInput = (List<Map<String, String>>) body.get("encontros");

            if (!"palestra".equals(tipo) && !"minicurso".equals(tipo)) {
                ctx.status(422);
                ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Tipo inválido"));
                return;
            }

            if (!(vagasObj instanceof Number)) {
                ctx.status(422);
                ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Vagas inválidas"));
                return;
            }
            int vagas = ((Number) vagasObj).intValue();
            if (vagas <= 0) {
                ctx.status(422);
                ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Vagas menores ou iguais a zero"));
                return;
            }

            try (Connection conn = Database.getConnection()) {
                PreparedStatement psSala = conn.prepareStatement("SELECT capacidade FROM salas WHERE id = ?");
                psSala.setString(1, salaId);
                ResultSet rsSala = psSala.executeQuery();
                if (!rsSala.next()) {
                    ctx.status(404);
                    ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Sala não encontrada"));
                    return;
                }
                int capacidadeSala = rsSala.getInt("capacidade");

                if (encontrosInput == null) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "QUANTIDADE_DE_ENCONTROS", "mensagem", "Encontros nulos"));
                    return;
                }

                class ParsedEnc {
                    OffsetDateTime inicio;
                    OffsetDateTime fim;
                }

                List<ParsedEnc> parsed = new ArrayList<>();
                for (Map<String, String> encMap : encontrosInput) {
                    String inicioStr = encMap.get("inicio");
                    String fimStr = encMap.get("fim");
                    if (inicioStr == null || fimStr == null) {
                        ctx.status(422);
                        ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Encontro sem início ou fim"));
                        return;
                    }
                    ParsedEnc pe = new ParsedEnc();
                    try {
                        pe.inicio = OffsetDateTime.parse(inicioStr);
                        pe.fim = OffsetDateTime.parse(fimStr);
                    } catch (Exception e) {
                        ctx.status(422);
                        ctx.json(Map.of("erro", "ENCONTRO_INVALIDO", "mensagem", "Data inválida"));
                        return;
                    }
                    parsed.add(pe);
                }

                // 1º Conflito de sala/horário (409 CONFLITO_DE_SALA)
                boolean salaConflito = false;
                PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT e.inicio, e.fim FROM encontros e " +
                    "JOIN atividades a ON e.atividadeId = a.id " +
                    "WHERE a.salaId = ? AND a.cancelada = 0"
                );
                psCheck.setString(1, salaId);
                ResultSet rsCheck = psCheck.executeQuery();
                List<ParsedEnc> existing = new ArrayList<>();
                while (rsCheck.next()) {
                    ParsedEnc pe = new ParsedEnc();
                    pe.inicio = OffsetDateTime.parse(rsCheck.getString("inicio"));
                    pe.fim = OffsetDateTime.parse(rsCheck.getString("fim"));
                    existing.add(pe);
                }

                for (ParsedEnc n : parsed) {
                    for (ParsedEnc ex : existing) {
                        if (n.inicio.isBefore(ex.fim) && ex.inicio.isBefore(n.fim)) {
                            salaConflito = true;
                            break;
                        }
                    }
                    if (salaConflito) break;
                }

                if (salaConflito) {
                    ctx.status(409);
                    ctx.json(Map.of("erro", "CONFLITO_DE_SALA", "mensagem", "Conflito de horário na sala"));
                    return;
                }

                // 2º Número de encontros e validade dos encontros (422 QUANTIDADE_DE_ENCONTROS / 422 ENCONTRO_INVALIDO)
                if ("palestra".equals(tipo) && parsed.size() != 1) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "QUANTIDADE_DE_ENCONTROS", "mensagem", "Palestra deve ter exatamente 1 encontro"));
                    return;
                }
                if ("minicurso".equals(tipo) && (parsed.size() < 2 || parsed.size() > 5)) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "QUANTIDADE_DE_ENCONTROS", "mensagem", "Minicurso deve ter entre 2 e 5 encontros"));
                    return;
                }

                for (int i = 0; i < parsed.size(); i++) {
                    ParsedEnc pe = parsed.get(i);
                    long mins = Duration.between(pe.inicio, pe.fim).toMinutes();
                    if (mins < 60 || mins > 240) {
                        ctx.status(422);
                        ctx.json(Map.of("erro", "ENCONTRO_INVALIDO", "mensagem", "Duração do encontro deve ser de 1 a 4 horas"));
                        return;
                    }
                    if (!pe.inicio.toLocalDate().equals(pe.fim.toLocalDate())) {
                        ctx.status(422);
                        ctx.json(Map.of("erro", "ENCONTRO_INVALIDO", "mensagem", "Encontro deve iniciar e terminar no mesmo dia"));
                        return;
                    }
                    for (int j = i + 1; j < parsed.size(); j++) {
                        ParsedEnc other = parsed.get(j);
                        if (pe.inicio.isBefore(other.fim) && other.inicio.isBefore(pe.fim)) {
                            ctx.status(422);
                            ctx.json(Map.of("erro", "ENCONTRO_INVALIDO", "mensagem", "Encontros da mesma atividade não podem se sobrepor"));
                            return;
                        }
                    }
                }

                // 3º Vagas acima da capacidade (422 VAGAS_ACIMA_DA_CAPACIDADE)
                if (vagas > capacidadeSala) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "VAGAS_ACIMA_DA_CAPACIDADE", "mensagem", "Vagas excedem a capacidade da sala"));
                    return;
                }

                String atvId = "atv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                try (PreparedStatement psIns = conn.prepareStatement("INSERT INTO atividades(id, titulo, tipo, salaId, vagas, cancelada) VALUES(?, ?, ?, ?, ?, 0)")) {
                    psIns.setString(1, atvId);
                    psIns.setString(2, titulo);
                    psIns.setString(3, tipo);
                    psIns.setString(4, salaId);
                    psIns.setInt(5, vagas);
                    psIns.executeUpdate();
                }

                for (ParsedEnc pe : parsed) {
                    String encId = "enc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                    try (PreparedStatement psEncIns = conn.prepareStatement("INSERT INTO encontros(id, atividadeId, inicio, fim) VALUES(?, ?, ?, ?)")) {
                        psEncIns.setString(1, encId);
                        psEncIns.setString(2, atvId);
                        psEncIns.setString(3, pe.inicio.toString());
                        psEncIns.setString(4, pe.fim.toString());
                        psEncIns.executeUpdate();
                    }
                }

                Map<String, Object> created = buildAtividade(conn, atvId);
                ctx.status(201);
                ctx.json(created);
            }
        });

        app.patch("/atividades/{id}", ctx -> {
            String xUsuario = ctx.header("X-Usuario");
            String role = Database.getUserRole(xUsuario);
            if (!"organizacao".equals(role)) {
                ctx.status(403);
                ctx.json(Map.of("erro", "SOMENTE_ORGANIZACAO", "mensagem", "Apenas organização"));
                return;
            }

            String id = ctx.pathParam("id");
            Map body;
            try {
                body = ctx.bodyAsClass(Map.class);
            } catch (Exception e) {
                ctx.status(422);
                ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Corpo inválido"));
                return;
            }

            if (body == null) {
                ctx.status(422);
                ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Corpo ausente"));
                return;
            }

            try (Connection conn = Database.getConnection()) {
                Map<String, Object> atv = buildAtividade(conn, id);
                if (atv == null) {
                    ctx.status(404);
                    ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Atividade não encontrada"));
                    return;
                }

                if ("cancelada".equals(atv.get("situacao"))) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "ATIVIDADE_CANCELADA", "mensagem", "Atividade cancelada não pode ser editada"));
                    return;
                }

                for (Object key : body.keySet()) {
                    String k = (String) key;
                    if (!"titulo".equals(k) && !"vagas".equals(k)) {
                        ctx.status(422);
                        ctx.json(Map.of("erro", "CAMPO_NAO_EDITAVEL", "mensagem", "Campo não editável"));
                        return;
                    }
                }

                String newTitulo = (String) body.get("titulo");
                Object newVagasObj = body.get("vagas");

                String sqlUpdate = "UPDATE atividades SET ";
                List<Object> params = new ArrayList<>();
                boolean hasUpdate = false;

                if (newTitulo != null) {
                    sqlUpdate += "titulo = ?";
                    params.add(newTitulo);
                    hasUpdate = true;
                }

                if (newVagasObj != null) {
                    if (!(newVagasObj instanceof Number)) {
                        ctx.status(422);
                        ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Vagas inválidas"));
                        return;
                    }
                    int newVagas = ((Number) newVagasObj).intValue();
                    if (newVagas <= 0) {
                        ctx.status(422);
                        ctx.json(Map.of("erro", "DADOS_INVALIDOS", "mensagem", "Vagas menores ou iguais a zero"));
                        return;
                    }

                    String salaId = (String) atv.get("salaId");
                    PreparedStatement psSala = conn.prepareStatement("SELECT capacidade FROM salas WHERE id = ?");
                    psSala.setString(1, salaId);
                    ResultSet rsSala = psSala.executeQuery();
                    if (rsSala.next()) {
                        int cap = rsSala.getInt("capacidade");
                        if (newVagas > cap) {
                            ctx.status(422);
                            ctx.json(Map.of("erro", "VAGAS_ACIMA_DA_CAPACIDADE", "mensagem", "Vagas acima da capacidade da sala"));
                            return;
                        }
                    }

                    int ocupadas = (int) atv.get("ocupadas");
                    if (newVagas < ocupadas) {
                        ctx.status(409);
                        ctx.json(Map.of("erro", "VAGAS_ABAIXO_DOS_INSCRITOS", "mensagem", "Vagas abaixo dos inscritos"));
                        return;
                    }

                    if (hasUpdate) {
                        sqlUpdate += ", ";
                    }
                    sqlUpdate += "vagas = ?";
                    params.add(newVagas);
                    hasUpdate = true;
                }

                if (hasUpdate) {
                    sqlUpdate += " WHERE id = ?";
                    params.add(id);
                    try (PreparedStatement psUp = conn.prepareStatement(sqlUpdate)) {
                        for (int i = 0; i < params.size(); i++) {
                            psUp.setObject(i + 1, params.get(i));
                        }
                        psUp.executeUpdate();
                    }
                }

                Map<String, Object> updated = buildAtividade(conn, id);
                ctx.json(updated);
            }
        });

        app.post("/atividades/{id}/cancelamento", ctx -> {
            String xUsuario = ctx.header("X-Usuario");
            String role = Database.getUserRole(xUsuario);
            if (!"organizacao".equals(role)) {
                ctx.status(403);
                ctx.json(Map.of("erro", "SOMENTE_ORGANIZACAO", "mensagem", "Apenas organização"));
                return;
            }

            String id = ctx.pathParam("id");
            try (Connection conn = Database.getConnection()) {
                Map<String, Object> atv = buildAtividade(conn, id);
                if (atv == null) {
                    ctx.status(404);
                    ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Atividade não encontrada"));
                    return;
                }

                if ("cancelada".equals(atv.get("situacao"))) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "ATIVIDADE_CANCELADA", "mensagem", "Atividade já cancelada"));
                    return;
                }

                String situacao = (String) atv.get("situacao");
                if ("em_andamento".equals(situacao) || "encerrada".equals(situacao)) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "ATIVIDADE_JA_INICIADA", "mensagem", "Atividade já iniciada"));
                    return;
                }

                try (PreparedStatement psUp = conn.prepareStatement("UPDATE atividades SET cancelada = 1 WHERE id = ?")) {
                    psUp.setString(1, id);
                    psUp.executeUpdate();
                }

                Map<String, Object> updated = buildAtividade(conn, id);
                ctx.json(updated);
            }
        });

        app.post("/atividades/{id}/inscricoes", ctx -> {
            String xUsuario = ctx.header("X-Usuario");
            String role = Database.getUserRole(xUsuario);
            if (!"participante".equals(role)) {
                ctx.status(403);
                ctx.json(Map.of("erro", "SOMENTE_PARTICIPANTE", "mensagem", "Apenas participante pode se inscrever"));
                return;
            }

            String atividadeId = ctx.pathParam("id");
            try (Connection conn = Database.getConnection()) {
                Map<String, Object> atv = buildAtividade(conn, atividadeId);
                if (atv == null) {
                    ctx.status(404);
                    ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Atividade não encontrada"));
                    return;
                }
                if ((Boolean) atv.getOrDefault("cancelada", false) || "cancelada".equals(atv.get("situacao"))) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "ATIVIDADE_CANCELADA", "mensagem", "Atividade cancelada"));
                    return;
                }

                List<Map<String, String>> encontros = (List<Map<String, String>>) atv.get("encontros");
                if (!encontros.isEmpty()) {
                    OffsetDateTime primeiroInicio = OffsetDateTime.parse(encontros.get(0).get("inicio"));
                    OffsetDateTime agora = Database.getClock();
                    if (!agora.isBefore(primeiroInicio.minusMinutes(30))) {
                        ctx.status(422);
                        ctx.json(Map.of("erro", "INSCRICOES_ENCERRADAS", "mensagem", "Inscrições encerradas"));
                        return;
                    }
                }

                PreparedStatement psCheck = conn.prepareStatement("SELECT 1 FROM inscricoes WHERE atividadeId = ? AND participanteId = ? AND status IN ('confirmada', 'em_espera', 'convocada')");
                psCheck.setString(1, atividadeId);
                psCheck.setString(2, xUsuario);
                ResultSet rsCheck = psCheck.executeQuery();
                if (rsCheck.next()) {
                    ctx.status(409);
                    ctx.json(Map.of("erro", "JA_INSCRITO", "mensagem", "Já inscrito"));
                    return;
                }

                int vagas = (Integer) atv.get("vagas");
                int ocupadas = (Integer) atv.get("ocupadas");
                boolean willOccupyVacancy = (ocupadas < vagas);

                if (willOccupyVacancy) {
                    List<Map<String, String>> encontrosNew = (List<Map<String, String>>) atv.get("encontros");
                    boolean hasConflict = false;

                    PreparedStatement psConflict = conn.prepareStatement(
                        "SELECT e.inicio, e.fim FROM encontros e " +
                        "JOIN inscricoes i ON e.atividadeId = i.atividadeId " +
                        "WHERE i.participanteId = ? AND i.status IN ('confirmada', 'convocada')"
                    );
                    psConflict.setString(1, xUsuario);
                    try (ResultSet rsConflict = psConflict.executeQuery()) {
                        while (rsConflict.next()) {
                            OffsetDateTime exStart = OffsetDateTime.parse(rsConflict.getString("inicio"));
                            OffsetDateTime exEnd = OffsetDateTime.parse(rsConflict.getString("fim"));
                            for (Map<String, String> ne : encontrosNew) {
                                OffsetDateTime nStart = OffsetDateTime.parse(ne.get("inicio"));
                                OffsetDateTime nEnd = OffsetDateTime.parse(ne.get("fim"));
                                if (nStart.isBefore(exEnd) && exStart.isBefore(nEnd)) {
                                    hasConflict = true;
                                    break;
                                }
                            }
                            if (hasConflict) break;
                        }
                    }

                    if (hasConflict) {
                        ctx.status(409);
                        ctx.json(Map.of("erro", "CONFLITO_DE_HORARIO", "mensagem", "Conflito de horário"));
                        return;
                    }
                }

                if ("minicurso".equals(atv.get("tipo"))) {
                    PreparedStatement psMiniCount = conn.prepareStatement(
                        "SELECT count(*) FROM inscricoes i " +
                        "JOIN atividades a ON i.atividadeId = a.id " +
                        "WHERE i.participanteId = ? AND i.status IN ('confirmada', 'convocada') AND a.tipo = 'minicurso'"
                    );
                    psMiniCount.setString(1, xUsuario);
                    try (ResultSet rsMini = psMiniCount.executeQuery()) {
                        if (rsMini.next() && rsMini.getInt(1) >= 3) {
                            ctx.status(422);
                            ctx.json(Map.of("erro", "LIMITE_DE_MINICURSOS", "mensagem", "Limite de minicursos atingido"));
                            return;
                        }
                    }
                }

                String status;
                Integer posicaoNaEspera = null;

                if (ocupadas < vagas) {
                    status = "confirmada";
                } else {
                    status = "em_espera";
                    PreparedStatement psEspera = conn.prepareStatement("SELECT count(*) FROM inscricoes WHERE atividadeId = ? AND status = 'em_espera'");
                    psEspera.setString(1, atividadeId);
                    ResultSet rsEspera = psEspera.executeQuery();
                    int countEspera = 0;
                    if (rsEspera.next()) {
                        countEspera = rsEspera.getInt(1);
                    }
                    posicaoNaEspera = countEspera + 1;
                }

                String insId = "ins_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                String criadaEm = Database.getClock().toString();

                PreparedStatement psIns = conn.prepareStatement("INSERT INTO inscricoes(id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm) VALUES(?, ?, ?, ?, ?, NULL, ?)");
                psIns.setString(1, insId);
                psIns.setString(2, atividadeId);
                psIns.setString(3, xUsuario);
                psIns.setString(4, status);
                if (posicaoNaEspera != null) {
                    psIns.setInt(5, posicaoNaEspera);
                } else {
                    psIns.setNull(5, java.sql.Types.INTEGER);
                }
                psIns.setString(6, criadaEm);
                psIns.executeUpdate();

                Map<String, Object> inscricao = new HashMap<>();
                inscricao.put("id", insId);
                inscricao.put("atividadeId", atividadeId);
                inscricao.put("participanteId", xUsuario);
                inscricao.put("status", status);
                inscricao.put("posicaoNaEspera", posicaoNaEspera);
                inscricao.put("convocadaAte", null);
                inscricao.put("criadaEm", criadaEm);

                ctx.status(201);
                ctx.json(inscricao);
            }
        });

        app.get("/inscricoes", ctx -> {
            String xUsuario = ctx.header("X-Usuario");
            String role = Database.getUserRole(xUsuario);
            String atividadeIdFiltro = ctx.queryParam("atividadeId");

            List<Map<String, Object>> lista = new ArrayList<>();
            try (Connection conn = Database.getConnection()) {
                String sql = "SELECT id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm FROM inscricoes WHERE 1=1";
                List<Object> params = new ArrayList<>();

                if ("participante".equals(role)) {
                    sql += " AND participanteId = ?";
                    params.add(xUsuario);
                }

                if (atividadeIdFiltro != null && !atividadeIdFiltro.isEmpty()) {
                    sql += " AND atividadeId = ?";
                    params.add(atividadeIdFiltro);
                }

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> ins = new HashMap<>();
                            ins.put("id", rs.getString("id"));
                            ins.put("atividadeId", rs.getString("atividadeId"));
                            ins.put("participanteId", rs.getString("participanteId"));
                            ins.put("status", rs.getString("status"));
                            int pos = rs.getInt("posicaoNaEspera");
                            if (rs.wasNull()) {
                                ins.put("posicaoNaEspera", null);
                            } else {
                                ins.put("posicaoNaEspera", pos);
                            }
                            ins.put("convocadaAte", rs.getString("convocadaAte"));
                            ins.put("criadaEm", rs.getString("criadaEm"));
                            lista.add(ins);
                        }
                    }
                }
            }
            ctx.json(lista);
        });

        app.post("/inscricoes/{id}/cancelamento", ctx -> {
            String xUsuario = ctx.header("X-Usuario");
            String role = Database.getUserRole(xUsuario);
            if (!"participante".equals(role)) {
                ctx.status(403);
                ctx.json(Map.of("erro", "SOMENTE_PARTICIPANTE", "mensagem", "Apenas participante"));
                return;
            }

            String id = ctx.pathParam("id");
            try (Connection conn = Database.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm FROM inscricoes WHERE id = ?");
                ps.setString(1, id);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    ctx.status(404);
                    ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Inscrição não encontrada"));
                    return;
                }

                String partId = rs.getString("participanteId");
                if (!partId.equals(xUsuario)) {
                    ctx.status(404);
                    ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Inscrição não encontrada"));
                    return;
                }

                String status = rs.getString("status");
                String atividadeId = rs.getString("atividadeId");

                if (!"confirmada".equals(status) && !"em_espera".equals(status) && !"convocada".equals(status)) {
                    ctx.status(422);
                    ctx.json(Map.of("erro", "INSCRICAO_INATIVA", "mensagem", "Inscrição já está inativa ou cancelada"));
                    return;
                }

                Map<String, Object> atv = buildAtividade(conn, atividadeId);
                if (atv != null) {
                    List<Map<String, String>> encontros = (List<Map<String, String>>) atv.get("encontros");
                    if (!encontros.isEmpty()) {
                        OffsetDateTime primeiroInicio = OffsetDateTime.parse(encontros.get(0).get("inicio"));
                        OffsetDateTime agora = Database.getClock();
                        if (!agora.isBefore(primeiroInicio)) {
                            ctx.status(422);
                            ctx.json(Map.of("erro", "ATIVIDADE_JA_INICIADA", "mensagem", "Atividade já iniciada"));
                            return;
                        }
                    }
                }

                try (PreparedStatement psUp = conn.prepareStatement("UPDATE inscricoes SET status = 'cancelada' WHERE id = ?")) {
                    psUp.setString(1, id);
                    psUp.executeUpdate();
                }

                Map<String, Object> ins = new HashMap<>();
                ins.put("id", rs.getString("id"));
                ins.put("atividadeId", atividadeId);
                ins.put("participanteId", partId);
                ins.put("status", "cancelada");
                int pos = rs.getInt("posicaoNaEspera");
                if (rs.wasNull()) {
                    ins.put("posicaoNaEspera", null);
                } else {
                    ins.put("posicaoNaEspera", pos);
                }
                ins.put("convocadaAte", rs.getString("convocadaAte"));
                ins.put("criadaEm", rs.getString("criadaEm"));
                ctx.json(ins);
            }
        });

        app.get("/inscricoes/{id}", ctx -> {
            String xUsuario = ctx.header("X-Usuario");
            String role = Database.getUserRole(xUsuario);
            String id = ctx.pathParam("id");
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id, atividadeId, participanteId, status, posicaoNaEspera, convocadaAte, criadaEm FROM inscricoes WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        ctx.status(404);
                        ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Inscrição não encontrada"));
                        return;
                    }
                    String partId = rs.getString("participanteId");
                    if ("participante".equals(role) && !partId.equals(xUsuario)) {
                        ctx.status(404);
                        ctx.json(Map.of("erro", "NAO_ENCONTRADO", "mensagem", "Inscrição não encontrada"));
                        return;
                    }

                    Map<String, Object> ins = new HashMap<>();
                    ins.put("id", rs.getString("id"));
                    ins.put("atividadeId", rs.getString("atividadeId"));
                    ins.put("participanteId", rs.getString("participanteId"));
                    ins.put("status", rs.getString("status"));
                    int pos = rs.getInt("posicaoNaEspera");
                    if (rs.wasNull()) {
                        ins.put("posicaoNaEspera", null);
                    } else {
                        ins.put("posicaoNaEspera", pos);
                    }
                    ins.put("convocadaAte", rs.getString("convocadaAte"));
                    ins.put("criadaEm", rs.getString("criadaEm"));
                    ctx.json(ins);
                }
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
