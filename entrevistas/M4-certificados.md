# Entrevista M4 — Certificados

> Documento de acompanhamento da entrevista de requisitos para o Módulo M4.
> Status: Entrevista concluída — todas as pendências respondidas.

---

## Decisões tomadas (Rodada 1)

### P1 — Encerramento da atividade
- **Pergunta:** Quando uma atividade é considerada encerrada para emissão do certificado?
- **Resposta:** A emissão só ocorre a partir do fim do último encontro da atividade.
- **Fonte:** RN-401.

### P2 — Frequência mínima
- **Pergunta:** Qual percentual ou quantidade de presença é suficiente para emitir certificado?
- **Resposta:** É necessário ter frequência mínima de 75% dos encontros, sem arredondar a favor; a forma inteira é `presenças × 4 >= encontros × 3`.
- **Fonte:** RN-404.

### P3 — Inscrição elegível
- **Pergunta:** O participante precisa estar confirmado, convocado ou pode estar em outro status?
- **Resposta:** Só participante com inscrição confirmada pode receber certificado.
- **Fonte:** RN-403.

### P4 — Carga horária e aproveitamento
- **Pergunta:** Como calcular carga horária, horas aproveitadas e a diferença entre palestra e minicurso?
- **Resposta:** O certificado usa a carga total da atividade. No extrato, palestras contam no máximo 240 minutos e o aproveitado total fica limitado a 1200 minutos.
- **Fonte:** RN-406, RN-411 e RN-412.

### P5 — Reemissão
- **Pergunta:** A emissão repetida deve retornar o mesmo certificado?
- **Resposta:** Sim. O código é criado na primeira emissão, nunca muda e a repetição retorna 200 com o mesmo certificado.
- **Fonte:** RN-407.

### P6 — Verificação pública
- **Pergunta:** Quais dados podem ser exibidos na verificação pública por código?
- **Resposta:** Sem autenticação, mostra código, nome abreviado, atividade, carga horária e data de emissão. Minúsculas no código devem ser aceitas.
- **Fonte:** RN-408 e RN-409.

### P7 — Atividades no extrato
- **Pergunta:** Quais atividades entram no extrato quando ainda não existe certificado?
- **Resposta:** Toda atividade elegível entra no extrato; quando ainda não há certificado, o código fica `null`.
- **Fonte:** RN-410.

## Rodada 2

As respostas foram conferidas no documento de requisitos e registradas com as fontes RN-401 a RN-413.
