# Entrevista M1 — Grade de Atividades

> Documento de acompanhamento da entrevista de requisitos para o Módulo M1.
> Status: Entrevista Concluída — Todas as Pendências Respondidas

---

## Decisões Tomadas (Rodada 1)

### P1 — Quantidade de Encontros e Encontros Válidos
- **Regra:** Palestras têm 1 encontro, minicursos têm de 2 a 5 encontros, duram de 1 a 4 horas, precisam terminar e começar no mesmo dia, e encontros de uma mesma atividade não se sobrepõem (`QUANTIDADE_DE_ENCONTROS`, `ENCONTRO_INVALIDO`).
- **Origem:** Resposta do usuário na consulta de requisitos.
- **Fonte:** Palestras tem 1 encontro, minicursos tem de 2 a 5 encontros, duram de 1 a 4 horas e precisam terminar e comecar no mesmo dia. Encontros de uma mesma atividade nao se sobrepoem.

### P2 — Limite de Vagas e Validação
- **Regra:** `vagas` não pode exceder a `capacidade` da sala informada. Se exceder, retorna `422 VAGAS_ACIMA_DA_CAPACIDADE`. `vagas` também não pode ser igual ou menor que zero (`vagas <= 0`), retornando `422 DADOS_INVALIDOS`.
- **Origem:** Resposta do usuário na Rodada 1.

### P3 — Conflito de Sala
- **Regra:** O encontro ou atividade que tentar marcar "por cima" de outro já existente/marcado na mesma sala e com sobreposição de horário não poderá ser criado/iniciado, retornando `409 CONFLITO_DE_SALA`.
- **Origem:** Resposta do usuário na Rodada 1.

### P4 — Edição de atividades (PATCH) e campos não editáveis
- **Regra:** Apenas `titulo` e `vagas` podem ser alterados no `PATCH /atividades/:id`. Tentar alterar outros campos retorna `422 CAMPO_NAO_EDITAVEL`.
- **Origem:** Resposta do usuário na consulta de requisitos.
- **Fonte:** So é possivel editar titulo e vagas.

### P5 — Redução de vagas no PATCH
- **Regra:** Vagas não podem ficar abaixo das inscrições que ocupam vaga (confirmadas + convocadas). Tentar reduzir abaixo desse limite retorna `409 VAGAS_ABAIXO_DOS_INSCRITOS`. Aumentar vagas convoca a lista de espera.
- **Origem:** Resposta do usuário na consulta de requisitos.
- **Fonte:** Vagas nao podem ficar abaixo das inscricoes que ocupam vaga (confirmadas +  convocadas). Aumentar vagas convoca espera.

### P7 — Cálculo de Carga Horária, Situação e Ocupação
- **Regra:** `cargaHorariaMinutos` é a soma da duração (fim - inicio) de todos os encontros em minutos. A `situacao` é calculada pelo relógio: `prevista` (antes do início do 1º encontro) → `em_andamento` (do início do 1º encontro até o fim do último) → `encerrada` (após o fim do último). `cancelada` prevalece sobre todas.
- **Origem:** Resposta do usuário na consulta de requisitos.
- **Fonte:** A situação é calculada pelo relógio: prevista → em_andamento (do início do 1º encontro) → encerrada (do fim do último); cancelada prevalece sobre todas.

### P8 — Filtros e Listagem GET /atividades
- **Regra:** `?dia` filtra atividades que possuem pelo menos um encontro no dia especificado; `?tipo` filtra por `palestra` ou `minicurso`. Se ambos forem informados, aplicam-se cumulativamente (AND). A listagem ordena pelo início do 1º encontro e, no empate, pelo título. Atividades canceladas aparecem na listagem.
- **Origem:** Resposta do usuário na consulta de requisitos.
- **Fonte:** A listagem ordena pelo início do 1º encontro; no empate, pelo título. Canceladas aparecem.

### P6 — Alteração e Cancelamento de Atividades Iniciadas ou Canceladas
- **Regra:** Não é permitido editar/cancelar atividades já canceladas (`422 ATIVIDADE_CANCELADA`) nem cancelar atividades que já iniciaram (`422 ATIVIDADE_JA_INICIADA`).
- **Origem:** Resposta do usuário na Rodada 1.

### P9 — Ordem de Precedência de Erros no POST /atividades
- **Regra:** A API realiza as validações em ordem (1º conflito de sala/horário, 2º número de encontros, 3º vagas acima da capacidade). Em caso de múltiplas violações, retorna o código de erro correspondente à primeira regra que falhou.
- **Origem:** Resposta do usuário na consulta de requisitos.
- **Fonte:** A API deve realizar as validações em ordem (1º conflito de sala/horário, 2º número de encontros, 3º vagas acima da capacidade). Em caso de múltiplas violações, deve retornar o código de erro correspondente à primeira regra que falhou, com o status HTTP definido em contrato-api.md. Fonte: contrato-api.md / Decisão de Projeto.
