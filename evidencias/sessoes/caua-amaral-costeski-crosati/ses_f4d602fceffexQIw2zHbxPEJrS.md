# TDD da fatia 1 de M1-grade.md em Javalin

| | |
|---|---|
| Sessão | `ses_f4d602fceffexQIw2zHbxPEJrS` |
| Pasta | Github_Facul/Trabalho_Final_Agentes_Skill_IA |
| Período | 18/09 00:46 → 18/09 00:54 |
| Modelo | google/gemini-3.5-flash-lite |
| Requisições ao modelo | 32 |
| Tokens de entrada / saída | 433.485 / 27.071 |
| Skills | tdd |
| Subagentes | — |
| Execuções de teste | 5 vermelhas, 2 verdes |
| TDD | 1 ciclo(s) vermelho → verde · 1 teste(s) que já nasceram verdes · 1 vez(es) teste e código juntos |
| Arquivos editados | 3 de teste, 6 de código, 0 de entrevista, 0 de spec, 0 de contexto, 0 de auditoria |
| Alertas | — |

## Linha do tempo

- `18/09 00:46` **prompt** — ``` Use a skill tdd. Implemente só a fatia 1 de specs/M1-grade.md. Um teste por vez: escreva o teste em JUnit 5, mostre ele falhando, e só então escreva o código no Javalin. ```
- `18/09 00:46` carrega a skill **tdd**
- `18/09 00:48` edita código `api/pom.xml`
- `18/09 00:48` edita teste `api/src/test/java/br/com/semanaacademica/Fatia1Test.java`
- `18/09 00:48` roda `& "C:\Program Files\NetBeans-12.0\netbeans\java\maven\bin\mvn.cmd" test` → **vermelho** — _teste e código mudaram juntos: não houve vermelho para ver_
- `18/09 00:49` edita código `api/src/main/java/br/com/semanaacademica/Database.java`
- `18/09 00:49` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 00:49` roda `& "C:\Program Files\NetBeans-12.0\netbeans\java\maven\bin\mvn.cmd" test` → **vermelho**
- `18/09 00:50` edita código `api/src/main/java/br/com/semanaacademica/Database.java`
- `18/09 00:50` roda `& "C:\Program Files\NetBeans-12.0\netbeans\java\maven\bin\mvn.cmd" test` → **vermelho** (0 passaram, 2 falharam)
- `18/09 00:50` edita teste `api/src/test/java/br/com/semanaacademica/Fatia1Test.java`
- `18/09 00:50` roda `& "C:\Program Files\NetBeans-12.0\netbeans\java\maven\bin\mvn.cmd" test` → **vermelho** (0 passaram, 1 falharam) — _teste novo falhando, como deve ser_
- `18/09 00:51` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 00:51` roda `& "C:\Program Files\NetBeans-12.0\netbeans\java\maven\bin\mvn.cmd" test` → **vermelho** (1 passaram, 1 falharam)
- `18/09 00:52` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 00:52` roda `& "C:\Program Files\NetBeans-12.0\netbeans\java\maven\bin\mvn.cmd" test` → verde (2 passaram) — _fecha um ciclo vermelho → verde_
- `18/09 00:53` edita teste `api/src/test/java/br/com/semanaacademica/Fatia1Test.java`
- `18/09 00:53` roda `& "C:\Program Files\NetBeans-12.0\netbeans\java\maven\bin\mvn.cmd" test` → verde (3 passaram) — _teste novo já nasceu verde_
