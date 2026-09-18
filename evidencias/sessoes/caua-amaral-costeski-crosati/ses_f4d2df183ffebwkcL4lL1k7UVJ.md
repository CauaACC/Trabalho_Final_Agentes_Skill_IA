# TDD da regra R3 com JUnit 5 e Javalin

| | |
|---|---|
| Sessão | `ses_f4d2df183ffebwkcL4lL1k7UVJ` |
| Pasta | Github_Facul/Trabalho_Final_Agentes_Skill_IA |
| Período | 18/09 01:41 → 18/09 01:48 |
| Modelo | google/gemini-3.5-flash-lite |
| Requisições ao modelo | 26 |
| Tokens de entrada / saída | 325.157 / 7.515 |
| Skills | tdd |
| Subagentes | — |
| Execuções de teste | 4 vermelhas, 2 verdes |
| TDD | 0 ciclo(s) vermelho → verde · 0 teste(s) que já nasceram verdes · 1 vez(es) teste e código juntos |
| Arquivos editados | 1 de teste, 2 de código, 0 de entrevista, 0 de spec, 0 de contexto, 0 de auditoria |
| Alertas | — |

## Linha do tempo

- `18/09 01:41` **prompt** — Use a skill tdd. Implemente a regra R3 de specs/M1-grade.md (recusa criação de atividade com encontros sobrepostos na mesma sala). Um teste por vez: escreva o teste em JUnit 5, mostre ele falhando, e só então escreva o código no Javalin.
- `18/09 01:41` carrega a skill **tdd**
- `18/09 01:41` roda `cd api && .\mvnw.cmd test` → **vermelho**
- `18/09 01:42` roda `.\mvnw.cmd test` → **vermelho**
- `18/09 01:42` roda `mvn test` → **vermelho**
- `18/09 01:45` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4t…` → verde (8 passaram)
- `18/09 01:46` edita teste `api/src/test/java/br/com/semanaacademica/Fatia2Test.java`
- `18/09 01:46` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 01:46` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4t…` → **vermelho** (6 passaram, 3 falharam) — _teste e código mudaram juntos: não houve vermelho para ver_
- `18/09 01:47` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 01:47` roda `& "C:\Users\caua\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4t…` → verde (9 passaram)
