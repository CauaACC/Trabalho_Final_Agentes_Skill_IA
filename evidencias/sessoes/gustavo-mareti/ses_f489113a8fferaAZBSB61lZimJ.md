# TDD da Fatia 3 de specs/M2-inscricoes.md

| | |
|---|---|
| Sessão | `ses_f489113a8fferaAZBSB61lZimJ` |
| Pasta | Trabalho_Final_Agentes_Skill_IA/Trabalho_Final_Agentes_Skill_IA |
| Período | 18/09 23:11 → 18/09 23:20 |
| Modelo | google/gemini-3.5-flash-lite |
| Requisições ao modelo | 26 |
| Tokens de entrada / saída | 466.458 / 17.725 |
| Skills | tdd |
| Subagentes | — |
| Execuções de teste | 2 vermelhas, 3 verdes |
| TDD | 1 ciclo(s) vermelho → verde · 0 teste(s) que já nasceram verdes · 0 vez(es) teste e código juntos |
| Arquivos editados | 1 de teste, 1 de código, 0 de entrevista, 0 de spec, 0 de contexto, 0 de auditoria |
| Alertas | — |

## Linha do tempo

- `18/09 23:11` **prompt** — Use a skill tdd. Implemente a Fatia 3 de specs/M2-inscricoes.md, focando no cancelamento de inscrições e fechamento de prazos (POST /inscricoes/:id/cancelamento, GET /inscricoes/:id). Escreva testes para assegurar que: 1) Inscrições fecham 30 minutos antes do início do 1º encontro (INSCRICOES_ENCERRADAS); 2) O participante pode cancelar a própria inscrição até a atividade começar (ATIVIDADE_JA_IN…
- `18/09 23:11` carrega a skill **tdd**
- `18/09 23:12` roda `.\mvnw.cmd test` → **vermelho**
- `18/09 23:12` roda `mvn test` → verde (29 passaram)
- `18/09 23:13` edita teste `api/src/test/java/br/com/semanaacademica/M2Fatia3Test.java`
- `18/09 23:13` roda `mvn test -Dtest=M2Fatia3Test` → **vermelho** (1 passaram, 2 falharam) — _teste novo falhando, como deve ser_
- `18/09 23:15` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 23:16` roda `mvn test -Dtest=M2Fatia3Test` → verde (3 passaram) — _fecha um ciclo vermelho → verde_
- `18/09 23:16` roda `mvn test` → verde (32 passaram)
