# Preenchimento de pendências em M1-grade.md

| | |
|---|---|
| Sessão | `ses_f4d781752ffePJxiMBIsV84I4N` |
| Pasta | Github_Facul/Trabalho_Final_Agentes_Skill_IA |
| Período | 18/09 00:20 → 18/09 00:35 |
| Modelo | google/gemini-3.5-flash-lite |
| Requisições ao modelo | 46 |
| Tokens de entrada / saída | 554.179 / 14.762 |
| Skills | — |
| Subagentes | — |
| Execuções de teste | 0 vermelhas, 0 verdes |
| TDD | 0 ciclo(s) vermelho → verde · 0 teste(s) que já nasceram verdes · 0 vez(es) teste e código juntos |
| Arquivos editados | 0 de teste, 0 de código, 13 de entrevista, 0 de spec, 0 de contexto, 0 de auditoria |
| Alertas | — |

## Linha do tempo

- `18/09 00:20` **prompt** — Vamos responder as perguntas marcadas como PENDENTE em entrevistas/M1-grade.md, uma de cada vez. Eu respondo com o que li no documento de requisitos, citando a regra; registre a minha resposta e ponha a regra na coluna Fonte. Não crie perguntas novas e não mexa nas perguntas já respondidas.
- `18/09 00:23` **prompt** — palestras tem 1 encontro, minicursos tem de 2 a 5 encontros, duram de 1 a 4 horas e precisam terminar e comecar no mesmo dia.encontros de uma mesma atividade nao se sobrepoem
- `18/09 00:24` edita entrevista `entrevistas/M1-grade.md` (3×)
- `18/09 00:25` **prompt** — so é possivel editar titulo e vagas
- `18/09 00:25` edita entrevista `entrevistas/M1-grade.md` (2×)
- `18/09 00:27` **prompt** — vagas nao podem ficar abaixo das inscricoes que ocupam vaga (confirmadas + convocadas). aumentar vagas convoca espera
- `18/09 00:27` edita entrevista `entrevistas/M1-grade.md` (2×)
- `18/09 00:28` **prompt** — A situação é **calculada pelo relógio**: `prevista` → `em_andamento` (do início do 1º encontro) → `encerrada` (do fim do último); `cancelada` prevalece sobre todas
- `18/09 00:28` edita entrevista `entrevistas/M1-grade.md` (2×)
- `18/09 00:29` **prompt** — A listagem ordena pelo início do 1º encontro; no empate, pelo título. Canceladas aparecem
- `18/09 00:29` edita entrevista `entrevistas/M1-grade.md` (2×)
- `18/09 00:34` **prompt** — A API deve realizar as validações em ordem (1º conflito de sala/horário, 2º número de encontros, 3º vagas acima da capacidade). Em caso de múltiplas violações, deve retornar o código de erro correspondente à primeira regra que falhou, com o status HTTP definido em contrato-api.md. Fonte: contrato-api.md / Decisão de Projeto.
- `18/09 00:34` edita entrevista `entrevistas/M1-grade.md` (2×)
