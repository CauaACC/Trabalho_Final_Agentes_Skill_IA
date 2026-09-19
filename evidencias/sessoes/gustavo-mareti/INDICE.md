# Sessões — Gustavo Mareti

Cada execução de teste é lida pelo que mudou desde a anterior:

- **Ciclo** — vermelho logo depois de mexer só em teste, e depois verde logo depois de mexer só em código. É o TDD.
- **Nasceu verde** — verde logo depois de mexer só em teste. Ou o comportamento já existia, ou o teste não testa o que diz.
- **Juntos** — teste e código mudaram antes da mesma execução. Não houve vermelho para ver.

**Alertas:** *colou* = prompt com 10 palavras seguidas ou mais iguais às do documento de requisitos (só aparece quando o resumo é gerado com `--requisitos`); *leu* = o agente acessou um arquivo de requisitos; *anexou* = o documento foi anexado à conversa.

Requisições são chamadas ao modelo: cada passo do agente é uma. Skills contam tanto a ferramenta `skill` quanto o comando `/nome`.

| Início | Sessão | Requisições | Skills | Subagentes | Vermelhas / verdes | Ciclos | Nasceu verde | Juntos | Alertas |
|---|---|---|---|---|---|---|---|---|---|
| 18/09 20:04 | [New session - 2026-09-18T23:04:43.171Z](ses_f493c285effeGMFV2HV4luch5G.md) | 10 | grilling | — | 0 / 0 | 0 | 0 | 0 | — |
| 18/09 21:51 | [Respostas pendentes em M2-inscricoes.md](ses_f48da4162ffe4YU9GnYB6SdL19.md) | 6 | — | — | 0 / 0 | 0 | 0 | 0 | — |
| 18/09 21:53 | [Criação de specs/M2-inscricoes.md via to-spec](ses_f48d89c5fffevN7ewOwKv4IOed.md) | 11 | to-spec | — | 0 / 0 | 0 | 0 | 0 | — |
| 18/09 22:31 | [TDD em POST /atividades/:id/inscricoes](ses_f48b56be3ffebnv6ln3OsTpmSq.md) | 28 | tdd | — | 1 / 3 | 1 | 1 | 0 | — |
| 18/09 22:59 | [TDD na Fatia 2 de M2-inscricoes.md](ses_f489c841effe1b9fqDB5OM0BnZ.md) | 21 | tdd | — | 3 / 2 | 0 | 0 | 1 | — |
| 18/09 23:11 | [TDD da Fatia 3 de specs/M2-inscricoes.md](ses_f489113a8fferaAZBSB61lZimJ.md) | 26 | tdd | — | 2 / 3 | 1 | 0 | 0 | — |
| 18/09 23:22 | [TDD para gestão de lista de espera na Fatia 4](ses_f4886fd87ffepKaUqhPPlYBRUD.md) | 33 | tdd | — | 2 / 1 | 1 | 0 | 0 | — |
| 18/09 23:39 | [Implementação TDD Fatia 5 em M2Fatia5Test.java](ses_f4877e4a0ffe1hdkrL9tvSPBNu.md) | 41 | tdd | — | 6 / 2 | 1 | 0 | 1 | — |
| 19/09 00:02 | [Auditoria M2 contra specs/M2-inscricoes.md](ses_f4862a6fcffedi6jY9JAmcsYyo.md) | 4 | — | auditor | 0 / 0 | 0 | 0 | 0 | — |
| 19/09 00:21 | [Criação de telas HTML e JS para Módulo M2](ses_f485117dcffewb60YHRMYiKEB6.md) | 36 | construir-telas | — | 1 / 0 | 0 | 0 | 0 | — |
| | **Total: 10 sessões** | 216 | grilling, to-spec, tdd (5), construir-telas | auditor | 15 / 11 | 4 | 1 | 2 | — |
