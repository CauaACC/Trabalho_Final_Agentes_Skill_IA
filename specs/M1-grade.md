# Spec — M1: Grade de Atividades

## 1. Objetivo
Gerenciar a grade de palestras e minicursos da Semana Acadêmica, permitindo que a organização cadastre atividades com encontros em salas válidas, consulte a programação com filtros e ordenação, realize alterações controladas e efetue cancelamentos.

## 2. Fora de escopo
- Inscrições de participantes e gerenciamento de lista de espera (M2).
- Registro de presença via QR code ou manual (M3).
- Emissão de certificados e extrato de horas (M4).
- Painel analítico da organização, verificação de participantes sem chance e bloqueios (M5).
- Cadastro de usuários e salas (utiliza dados iniciais fixos).

## 3. Modelo

### Sala
- `id`: identificador da sala (informado / dados iniciais).
- `nome`: nome da sala (informado / dados iniciais).
- `capacidade`: capacidade máxima de lotação (informado / dados iniciais).

### Atividade
- `id`: identificador gerado (`atv_...`, gerado pela API).
- `titulo`: título da atividade (informado pelo cliente).
- `tipo`: tipo da atividade (`palestra` ou `minicurso`, informado pelo cliente).
- `salaId`: identificador da sala alocada (informado pelo cliente).
- `vagas`: número máximo de vagas oferecidas (informado pelo cliente).
- `encontros`: lista de encontros da atividade, ordenada por início (informado pelo cliente).
- `cargaHorariaMinutos`: soma da duração de todos os encontros em minutos (derivado/calculado).
- `situacao`: estado atual da atividade (`prevista`, `em_andamento`, `encerrada`, `cancelada`, calculado).
- `ocupadas`: número de inscrições que ocupam vaga (calculado).
- `vagasRestantes`: vagas disponíveis para nova inscrição (calculado).
- `emEspera`: número de inscrições na lista de espera (calculado).

### Encontro
- `id`: identificador gerado (`enc_...`, gerado pela API).
- `inicio`: data e hora de início em formato ISO 8601 com fuso (informado pelo cliente).
- `fim`: data e hora de término em formato ISO 8601 com fuso (informado pelo cliente).

## 4. Endpoints

| Método | Rota | Quem | Sucesso |
|---|---|---|---|
| GET | `/salas` | todos | 200 `[Sala]` |
| GET | `/atividades` | todos | 200 `[Atividade]` — filtros `?dia=AAAA-MM-DD` e `?tipo=palestra\|minicurso` |
| GET | `/atividades/:id` | todos | 200 `Atividade` |
| POST | `/atividades` | organização | 201 `Atividade` |
| PATCH | `/atividades/:id` | organização | 200 `Atividade` |
| POST | `/atividades/:id/cancelamento` | organização | 200 `Atividade` |

## 5. Regras

- **R1 (P1):** Palestras devem possuir exatamente 1 encontro; minicursos devem possuir de 2 a 5 encontros. Cada encontro deve durar de 1 a 4 horas e precisa iniciar e terminar no mesmo dia calendarizado. Encontros de uma mesma atividade não podem se sobrepor temporalmente. Recusa: `422 QUANTIDADE_DE_ENCONTROS` ou `422 ENCONTRO_INVALIDO`.
- **R2 (P2):** O número de `vagas` informado não pode exceder a capacidade da sala alocada. Recusa: `422 VAGAS_ACIMA_DA_CAPACIDADE`. Valores de `vagas` menores ou iguais a zero (`vagas <= 0`) recusam com `422 DADOS_INVALIDOS`.
- **R3 (P3):** Não é permitida a criação de atividade ou encontro cujo horário sobreponha-se a outro encontro já marcado na mesma sala. Recusa: `409 CONFLITO_DE_SALA`.
- **R4 (P4):** No `PATCH /atividades/:id`, apenas os campos `titulo` e `vagas` podem ser alterados. Tentar alterar outros campos (como `tipo`, `salaId`, `encontros`) recusa com `422 CAMPO_NAO_EDITAVEL`.
- **R5 (P5):** No `PATCH /atividades/:id`, o número de `vagas` não pode ser reduzido abaixo do total de inscrições que ocupam vaga (confirmadas + convocadas). Tentar reduzir abaixo desse limite recusa com `409 VAGAS_ABAIXO_DOS_INSCRITOS`. Aumentar as vagas convoca automaticamente participantes da lista de espera.
- **R6 (P6):** Não é permitido editar ou cancelar atividades que já se encontram canceladas (recusa `422 ATIVIDADE_CANCELADA`). Não é permitido cancelar atividades que já iniciaram (recusa `422 ATIVIDADE_JA_INICIADA`).
- **R7 (P7):** A `cargaHorariaMinutos` é a soma da duração (`fim - inicio`) de todos os encontros em minutos. A `situacao` é calculada dinamicamente pelo relógio do sistema: `prevista` (antes do início do 1º encontro) → `em_andamento` (do início do 1º encontro até o término do último) → `encerrada` (após o término do último). A situação `cancelada` prevalece sobre todas as demais.
- **R8 (P8):** A listagem `GET /atividades` suporta filtros opcionais `?dia=AAAA-MM-DD` (atividades com pelo menos um encontro no dia especificado) e `?tipo=palestra|minicurso`. Quando ambos forem informados, aplicam-se cumulativamente (AND). A listagem é ordenada pelo início do primeiro encontro e, em caso de empate, pelo título em ordem alfabética. Atividades canceladas devem aparecer na listagem.
- **R9 (P9):** A API realiza as validações de criação no `POST /atividades` na seguinte ordem de precedência: 1º conflito de sala/horário (`409 CONFLITO_DE_SALA`), 2º número de encontros e validade dos encontros (`422 QUANTIDADE_DE_ENCONTROS` / `422 ENCONTRO_INVALIDO`), 3º vagas acima da capacidade (`422 VAGAS_ACIMA_DA_CAPACIDADE`). Em caso de múltiplas violações, retorna o código correspondente à primeira regra que falhou.

## 6. Critérios de aceite

1. (R1) `POST /atividades` com palestra contendo 2 encontros → `422 QUANTIDADE_DE_ENCONTROS`.
2. (R1) `POST /atividades` com minicurso contendo encontros com duração de 5 horas → `422 ENCONTRO_INVALIDO`.
3. (R2) `POST /atividades` com vagas excedendo a capacidade da sala → `422 VAGAS_ACIMA_DA_CAPACIDADE`.
4. (R3) `POST /atividades` em sala e horário já ocupados por outra atividade → `409 CONFLITO_DE_SALA`.
5. (R4) `PATCH /atividades/:id` tentando alterar a sala ou encontros → `422 CAMPO_NAO_EDITAVEL`.
6. (R5) `PATCH /atividades/:id` reduzindo vagas abaixo do número de inscritos ativos → `409 VAGAS_ABAIXO_DOS_INSCRITOS`.
7. (R6) `POST /atividades/:id/cancelamento` em atividade já iniciada → `422 ATIVIDADE_JA_INICIADA`.
8. (R7) `GET /atividades/:id` retorna `cargaHorariaMinutos` correta e `situacao` conforme o relógio.
9. (R8) `GET /atividades?dia=2026-10-19&tipo=minicurso` retorna apenas minicursos do dia ordenados por início e título.
10. (R9) `POST /atividades` violando simultaneamente conflito de sala e número de encontros retorna `409 CONFLITO_DE_SALA` (1ª precedência).

## 7. Como isto será verificado
Testes automatizados de API em JUnit 5 utilizando Javalin e SQLite, executados em modo de teste (`MODO_TESTE=1`) com controle explícito do relógio via `PUT /_teste/relogio` e limpeza via `POST /_teste/reset`.

## 8. Fatias de entrega

- **Fatia 1:** Consulta e Listagem (`GET /salas`, `GET /atividades`, `GET /atividades/:id`, cálculo de carga horária e situação).
- **Fatia 2:** Criação de Atividades (`POST /atividades`, validações de encontros, capacidade da sala, conflito de sala e ordem de precedência).
- **Fatia 3:** Alteração e Cancelamento (`PATCH /atividades/:id`, restrições de campos editáveis, proteção contra redução de vagas abaixo dos inscritos, cancelamento de atividades).
