# Entrevista M2 — Inscrições e Lista de Espera

> Documento de acompanhamento da entrevista de requisitos para o Módulo M2.
> Status: Entrevista Concluída — Todas as Decisões Tomadas

---

## Decisões Tomadas

### P1 — Janela de Inscrição
- **Regra:** As inscrições encerram 30 minutos antes do início do primeiro encontro da atividade (`422 INSCRICOES_ENCERRADAS`).
- **Origem:** Resposta do usuário.

### P2 — Limite de Minicursos
- **Regra:** O participante pode ter no máximo 3 minicursos ocupando vaga (`confirmada` ou `convocada`). Palestras não contam para o limite; inscrições na lista de espera (`em_espera`) também não contam (`422 LIMITE_DE_MINICURSOS`).
- **Origem:** Resposta do usuário.

### P3 — Conflito de Horário
- **Regra:** Ao ocupar vaga (`confirmada` ou `convocada`), o participante não pode ter outra inscrição que ocupe vaga com encontro sobreposto. **Encostar horários (fim de um igual ao início de outro) não é conflito.** Quem está apenas na lista de espera (`em_espera`) não passa por verificação de conflito de horário (`409 CONFLITO_DE_HORARIO`).
- **Origem:** Resposta do usuário.

### P4 — Confirmação de Convocação e Erros
- **Regra:** Tentar confirmar convocação com prazo vencido retorna `422 CONVOCACAO_EXPIRADA`; tentar confirmar inscrição que não está com status `convocada` retorna `422 SEM_CONVOCACAO`.
- **Origem:** Resposta do usuário.

### P5 — Cancelamento de Inscrição
- **Regra:** Não é permitido cancelar inscrições se a atividade já iniciou (`422 ATIVIDADE_JA_INICIADA`) ou se a inscrição já estiver inativa/cancelada (`422 INSCRICAO_INATIVA`).
- **Origem:** Resposta do usuário.

### P6 — Prazo de Expiração da Convocação
- **Regra:** O prazo de expiração da convocação (`convocadaAte`) é de 2 horas a partir do momento da convocação (ou até o início do primeiro encontro da atividade, o que ocorrer primeiro).
- **Origem:** Resposta do usuário (Opção C: 2h).

### P7 — Ordem de Precedência de Erros no POST /atividades/:id/inscricoes
- **Regra:** A ordem de verificação e erro retornado é: 1º Atividade Cancelada / Inscrições Encerradas / Bloqueada, 2º Já Inscrito, 3º Conflito de Horário, 4º Limite de Minicursos.
- **Origem:** Resposta do usuário.



