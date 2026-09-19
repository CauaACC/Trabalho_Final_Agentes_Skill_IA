# TDD em POST /atividades/:id/inscricoes

| | |
|---|---|
| Sessão | `ses_f48b56be3ffebnv6ln3OsTpmSq` |
| Pasta | Trabalho_Final_Agentes_Skill_IA/Trabalho_Final_Agentes_Skill_IA |
| Período | 18/09 22:31 → 18/09 22:46 |
| Modelo | google/gemini-3.5-flash-lite |
| Requisições ao modelo | 28 |
| Tokens de entrada / saída | 917.666 / 17.810 |
| Skills | tdd |
| Subagentes | — |
| Execuções de teste | 1 vermelhas, 3 verdes |
| TDD | 1 ciclo(s) vermelho → verde · 1 teste(s) que já nasceram verdes · 0 vez(es) teste e código juntos |
| Arquivos editados | 3 de teste, 1 de código, 0 de entrevista, 0 de spec, 0 de contexto, 0 de auditoria |
| Alertas | — |

## Linha do tempo

- `18/09 22:31` **prompt** — Use a skill tdd. Implemente a Fatia 1 de specs/M2-inscricoes.md, focando na rota POST /atividades/:id/inscricoes. Primeiro, escreva um teste automatizado que garanta que apenas participantes podem se inscrever (SOMENTE_PARTICIPANTE) e que verifique se uma inscrição nasce como confirmada (se houver vagas) ou em_espera no fim da fila (se lotada). Execute o teste para provar que ele falha, implement…
- `18/09 22:31` carrega a skill **tdd**
- `18/09 22:33` roda `mvn test` → verde (21 passaram)
- `18/09 22:33` edita teste `api/src/test/java/br/com/semanaacademica/Fatia1Test.java`
- `18/09 22:33` roda `mvn test` → **vermelho** (21 passaram, 2 falharam) — _teste novo falhando, como deve ser_
- `18/09 22:35` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 22:36` roda `mvn test` → verde (23 passaram) — _fecha um ciclo vermelho → verde_
- `18/09 22:43` **prompt** — Ao invés de adicionar na classe Fatia1Test.java, crie uma nova classe com o teste chamada M2Fatia1Test.java
- `18/09 22:43` edita teste `api/src/test/java/br/com/semanaacademica/M2Fatia1Test.java`
- `18/09 22:44` edita teste `api/src/test/java/br/com/semanaacademica/Fatia1Test.java`
- `18/09 22:45` roda `mvn test` → verde (23 passaram) — _teste novo já nasceu verde_
