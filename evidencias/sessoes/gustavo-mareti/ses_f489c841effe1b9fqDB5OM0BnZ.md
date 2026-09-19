# TDD na Fatia 2 de M2-inscricoes.md

| | |
|---|---|
| Sessão | `ses_f489c841effe1b9fqDB5OM0BnZ` |
| Pasta | Trabalho_Final_Agentes_Skill_IA/Trabalho_Final_Agentes_Skill_IA |
| Período | 18/09 22:59 → 18/09 23:06 |
| Modelo | google/gemini-3.5-flash-lite |
| Requisições ao modelo | 21 |
| Tokens de entrada / saída | 463.219 / 23.750 |
| Skills | tdd |
| Subagentes | — |
| Execuções de teste | 3 vermelhas, 2 verdes |
| TDD | 0 ciclo(s) vermelho → verde · 0 teste(s) que já nasceram verdes · 1 vez(es) teste e código juntos |
| Arquivos editados | 3 de teste, 1 de código, 0 de entrevista, 0 de spec, 0 de contexto, 0 de auditoria |
| Alertas | — |

## Linha do tempo

- `18/09 22:59` **prompt** — Use a skill tdd. Implemente a Fatia 2 de specs/M2-inscricoes.md, focando nas restrições ao ocupar vaga. Escreva testes para garantir que: 1) Um participante não pode ter inscrições ativas que ocupem vaga com horários sobrepostos (CONFLITO_DE_HORARIO, lembrando que encostar não é conflito e espera não conta); 2) Há o limite de no máximo 3 minicursos ocupando vaga (LIMITE_DE_MINICURSOS, onde palest…
- `18/09 22:59` carrega a skill **tdd**
- `18/09 22:59` roda `mvn test` → verde (23 passaram)
- `18/09 23:00` edita teste `api/src/test/java/br/com/semanaacademica/M2Fatia2Test.java`
- `18/09 23:00` roda `mvn test` → **vermelho** (23 passaram, 1 falharam) — _teste novo falhando, como deve ser_
- `18/09 23:01` edita teste `api/src/test/java/br/com/semanaacademica/M2Fatia2Test.java`
- `18/09 23:01` roda `mvn test` → **vermelho** (25 passaram, 2 falharam) — _teste novo falhando, como deve ser_
- `18/09 23:02` roda `mvn test -Dtest=M2Fatia2Test` → **vermelho** (2 passaram, 2 falharam)
- `18/09 23:02` edita teste `api/src/test/java/br/com/semanaacademica/M2Fatia2Test.java`
- `18/09 23:03` edita código `api/src/main/java/br/com/semanaacademica/Main.java`
- `18/09 23:04` roda `mvn test` → verde (29 passaram) — _teste e código mudaram juntos: não houve vermelho para ver_
