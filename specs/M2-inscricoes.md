# Spec — M2: Inscrições e Lista de Espera

## 1. Objetivo
Gerenciar as inscrições de participantes em palestras e minicursos, controle de vagas e lotação, gerenciamento de lista de espera (`em_espera`), confirmação de convocações com prazo limite, e cancelamentos controlados.

## 2. Fora de escopo
- Criação e alocação de atividades e salas (M1).
- Registro de presença via QR code ou manual (M3).
- Emissão de certificados e extrato de horas (M4).
- Painel analítico da organização, verificação de participantes sem chance e bloqueios (M5).
- Cadastro de usuários e salas (utiliza dados iniciais fixos).

## 3. Modelo

### Inscrição
- `id`: identificador gerado (`ins_...`, gerado pela API).
- `atividadeId`: identificador da atividade (informado pelo cliente).
- `participanteId`: identificador do participante (informado pelo cabeçalho `X-Usuario`).
- `status`: estado atual da inscrição (`confirmada`, `em_espera`, `convocada`, `cancelada`, `expirada`, calculado).
- `posicaoNaEspera`: número da posição na fila de espera (1, 2, ...), preenchido apenas quando `em_espera` (calculado).
- `convocadaAte`: data e hora limite para confirmação da convocação em formato ISO 8601 com fuso, preenchido apenas quando `convocada` (calculado).
- `criadaEm`: data e hora de criação da inscrição (calculado/registrado).

## 4. Endpoints

| Método | Rota | Quem | Sucesso |
|---|---|---|---|
| POST | `/atividades/:id/inscricoes` | participante | 201 `Inscricao` (sem corpo na entrada) |
| GET | `/inscricoes` | todos | 200 `[Inscricao]` — participante recebe só as próprias; filtro `?atividadeId=` |
| GET | `/inscricoes/:id` | todos | 200 `Inscricao` |
| POST | `/inscricoes/:id/cancelamento` | participante | 200 `Inscricao` |
| POST | `/inscricoes/:id/confirmacao` | participante | 200 `Inscricao` |

## 5. Regras

- **R1 (P1):** As inscrições encerram 30 minutos antes do início do primeiro encontro da atividade. Recusa: `422 INSCRICOES_ENCERRADAS`.
- **R2 (P2):** O participante pode ter no máximo 3 minicursos ocupando vaga (`confirmada` ou `convocada`). Palestras não contam para o limite; inscrições na lista de espera (`em_espera`) também não contam. Recusa: `422 LIMITE_DE_MINICURSOS`.
- **R3 (P3):** Ao ocupar vaga (`confirmada` ou `convocada`), o participante não pode ter outra inscrição que ocupe vaga com encontro sobreposto. Encostar horários (fim de um igual ao início de outro) não é conflito. Quem está apenas na lista de espera (`em_espera`) não passa por verificação de conflito de horário. Recusa: `409 CONFLITO_DE_HORARIO`.
- **R4 (P4):** Tentar confirmar convocação com prazo vencido retorna `422 CONVOCACAO_EXPIRADA`; tentar confirmar inscrição que não está com status `convocada` retorna `422 SEM_CONVOCACAO`.
- **R5 (P5):** Não é permitido cancelar inscrições se a atividade já iniciou (`422 ATIVIDADE_JA_INICIADA`) ou se a inscrição já estiver inativa/cancelada (`422 INSCRICAO_INATIVA`).
- **R6 (P6):** O prazo de expiração da convocação (`convocadaAte`) é de 2 horas a partir do momento da convocação (ou até o início do primeiro encontro da atividade, o que ocorrer primeiro).
- **R7 (P7):** A API realiza as validações de inscrição no `POST /atividades/:id/inscricoes` na seguinte ordem de precedência: 1º Atividade Cancelada (`422 ATIVIDADE_CANCELADA`) / Inscrições Encerradas (`422 INSCRICOES_ENCERRADAS`) / Bloqueada (`422 INSCRICAO_BLOQUEADA`), 2º Já Inscrito (`409 JA_INSCRITO`), 3º Conflito de Horário (`409 CONFLITO_DE_HORARIO`), 4º Limite de Minicursos (`422 LIMITE_DE_MINICURSOS`).

## 6. Critérios de aceite

1. (R1) `POST /atividades/:id/inscricoes` realizada a menos de 30 minutos do início do primeiro encontro → `422 INSCRICOES_ENCERRADAS`.
2. (R2) `POST /atividades/:id/inscricoes` em minicurso quando o participante já possui 3 minicursos com vaga ocupada → `422 LIMITE_DE_MINICURSOS`.
3. (R3) `POST /atividades/:id/inscricoes` em atividade cujos encontros sobrepõem-se a outra atividade com vaga ocupada → `409 CONFLITO_DE_HORARIO`.
4. (R4) `POST /inscricoes/:id/confirmacao` com prazo de convocação vencido → `422 CONVOCACAO_EXPIRADA`.
5. (R4) `POST /inscricoes/:id/confirmacao` em inscrição com status diferente de `convocada` → `422 SEM_CONVOCACAO`.
6. (R5) `POST /inscricoes/:id/cancelamento` em inscrição de atividade que já iniciou → `422 ATIVIDADE_JA_INICIADA`.
7. (R5) `POST /inscricoes/:id/cancelamento` em inscrição já cancelada ou inativa → `422 INSCRICAO_INATIVA`.
8. (R6) Convocação gerada define `convocadaAte` respeitando o teto de 2 horas ou o início do 1º encontro.
9. (R7) `POST /atividades/:id/inscricoes` violando simultaneamente `JA_INSCRITO` e `CONFLITO_DE_HORARIO` retorna `409 JA_INSCRITO` (1ª precedência).

## 7. Como isto será verificado
Testes automatizados de API em JUnit 5 utilizando Javalin e SQLite, executados em modo de teste (`MODO_TESTE=1`) com controle explícito do relógio via `PUT /_teste/relogio` e limpeza via `POST /_teste/reset`.

## 8. Fatias de entrega

- **Fatia 1:** Inscrições e Lista de Espera Básica (`POST /atividades/:id/inscricoes`, `GET /inscricoes`, `GET /inscricoes/:id`, janela de 30 min, verificação de vaga/lotação, entrada na fila de espera com `posicaoNaEspera`).
- **Fatia 2:** Limitações e Conflitos (`LIMITE_DE_MINICURSOS`, `CONFLITO_DE_HORARIO`, ordem de precedência).
- **Fatia 3:** Convocação, Confirmação e Cancelamento (`POST /inscricoes/:id/confirmacao`, prazos de convocação `convocadaAte`, `CONVOCACAO_EXPIRADA`, `SEM_CONVOCACAO`, cancelamento de inscrições e re-convocação automática).
