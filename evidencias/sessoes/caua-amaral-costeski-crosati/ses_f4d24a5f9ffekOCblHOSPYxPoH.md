# TDD: Regra R4 PATCH /atividades/:id no Javalin

| | |
|---|---|
| Sessão | `ses_f4d24a5f9ffekOCblHOSPYxPoH` |
| Pasta | Github_Facul/Trabalho_Final_Agentes_Skill_IA |
| Período | 18/09 01:51 → 18/09 01:56 |
| Modelo | google/gemini-3.5-flash-lite |
| Requisições ao modelo | 24 |
| Tokens de entrada / saída | 437.013 / 13.048 |
| Skills | tdd |
| Subagentes | — |
| Execuções de teste | 3 vermelhas, 2 verdes |
| TDD | 1 ciclo(s) vermelho → verde · 1 teste(s) que já nasceram verdes · 0 vez(es) teste e código juntos |
| Arquivos editados | 2 de teste, 1 de código, 0 de entrevista, 0 de spec, 0 de contexto, 0 de auditoria |
| Alertas | — |

## Linha do tempo

- `18/09 01:51` **prompt** — Use a skill tdd. Implemente a regra R4 de specs/M1-grade.md (no PATCH /atividades/:id apenas titulo e vagas podem ser alterados, recusando outros campos com 422 CAMPO\_NAO\_EDITAVEL). Um teste por vez: escreva o teste em JUnit 5, mostre ele falhando, e só então escreva o código no Javalin.
- `18/09 01:51` carrega a skill **tdd**
- `18/09 01:52` edita teste `api/src/test/java/br/com/semanaacademica/Fatia3Test.java`
- `18/09 01:53` roda `.\mvnw.cmd test` → **vermelho** — _teste novo falhando, como deve ser_
- `18/09 01:53` roda `mvn test` → **vermelho**
- `18/09 01:53` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11\03d7e36a140982eea48e22c1…` → **vermelho** (9 passaram, 1 falharam)
- `18/09 01:54` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 01:54` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11\03d7e36a140982eea48e22c1…` → verde (10 passaram) — _fecha um ciclo vermelho → verde_
- `18/09 01:55` edita teste `api/src/test/java/br/com/semanaacademica/Fatia3Test.java`
- `18/09 01:56` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11\03d7e36a140982eea48e22c1…` → verde (11 passaram) — _teste novo já nasceu verde_
