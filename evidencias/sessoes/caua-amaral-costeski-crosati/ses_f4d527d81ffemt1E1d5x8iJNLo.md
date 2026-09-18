# TDD fatia 2 specs/M1-grade.md em Javalin

| | |
|---|---|
| Sessão | `ses_f4d527d81ffemt1E1d5x8iJNLo` |
| Pasta | Github_Facul/Trabalho_Final_Agentes_Skill_IA |
| Período | 18/09 01:01 → 18/09 01:11 |
| Modelo | google/gemini-3.5-flash-lite |
| Requisições ao modelo | 33 |
| Tokens de entrada / saída | 355.109 / 19.581 |
| Skills | tdd |
| Subagentes | — |
| Execuções de teste | 3 vermelhas, 3 verdes |
| TDD | 1 ciclo(s) vermelho → verde · 2 teste(s) que já nasceram verdes · 0 vez(es) teste e código juntos |
| Arquivos editados | 3 de teste, 2 de código, 0 de entrevista, 0 de spec, 0 de contexto, 0 de auditoria |
| Alertas | — |

## Linha do tempo

- `18/09 01:01` **prompt** — Use a skill tdd. Implemente só a fatia 2 de specs/M1-grade.md. Um teste por vez: escreva o teste em JUnit 5, mostre ele falhando, e só então escreva o código no Javalin.
- `18/09 01:02` carrega a skill **tdd**
- `18/09 01:04` edita teste `api/src/test/java/br/com/semanaacademica/Fatia2Test.java`
- `18/09 01:04` roda `.\mvnw.cmd test` → **vermelho** — _teste novo falhando, como deve ser_
- `18/09 01:04` roda `mvn test` → **vermelho**
- `18/09 01:07` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4t…` → **vermelho** (3 passaram, 1 falharam)
- `18/09 01:07` edita código `api/src/main/java/br/com/semanaacademica/Database.java`
- `18/09 01:08` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 01:08` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4t…` → verde (4 passaram) — _fecha um ciclo vermelho → verde_
- `18/09 01:09` edita teste `api/src/test/java/br/com/semanaacademica/Fatia2Test.java`
- `18/09 01:09` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4t…` → verde (5 passaram) — _teste novo já nasceu verde_
- `18/09 01:09` edita teste `api/src/test/java/br/com/semanaacademica/Fatia2Test.java`
- `18/09 01:09` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4t…` → verde (8 passaram) — _teste novo já nasceu verde_
