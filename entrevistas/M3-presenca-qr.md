# Entrevista M3 — Presença por QR

> Documento de acompanhamento da entrevista de requisitos para o Módulo M3.
> Status: Entrevista concluída — todas as pendências respondidas.

---

## Decisões tomadas (Rodada 1)

### P1 — Troca e validade do código
- **Pergunta:** Com quanto tempo o código do encontro deve trocar e por quanto tempo permanece válido?
- **Resposta:** O código muda a cada minuto, em janelas alinhadas ao relógio. São aceitos o código do minuto atual e o do minuto anterior.
- **Fonte:** RN-303 e RN-304.

### P2 — Janela de presença
- **Pergunta:** Em que momento o participante pode registrar presença?
- **Resposta:** A janela vai de 15 minutos antes até 30 minutos depois do início do encontro, com as bordas incluídas.
- **Fonte:** RN-301.

### P3 — Leitura offline
- **Pergunta:** O que caracteriza uma leitura offline válida e qual é o limite para sincronização?
- **Resposta:** A regra usa o instante `lidoEm`; o envio pode ocorrer até 2 horas depois do fim do encontro. Depois disso é sincronização tardia.
- **Fonte:** RN-308, RN-309 e RN-310.

### P4 — Repetição de presença
- **Pergunta:** Uma segunda leitura do mesmo participante deve criar outro registro?
- **Resposta:** Não. A presença é única por participante e encontro; a repetição retorna 200 com a mesma presença antes das demais regras.
- **Fonte:** RN-307.

### P5 — Perfis e permissões
- **Pergunta:** Quais perfis podem gerar o código, registrar presença e consultar a lista?
- **Resposta:** A organização gera o código e consulta a lista; o participante inscrito registra a própria presença.
- **Fonte:** RN-302 e RN-306.

### P6 — Presença manual
- **Pergunta:** A presença manual exige justificativa e existe limite por participante/encontro?
- **Resposta:** A organização pode registrar manualmente para participante confirmado, com justificativa de pelo menos 10 caracteres. O limite é 10% das inscrições confirmadas, arredondando para cima, por encontro.
- **Fonte:** RN-311 e RN-313.

### P7 — Cancelamento e inscrição
- **Pergunta:** O que acontece quando a atividade está cancelada ou o participante não está inscrito?
- **Resposta:** Atividade cancelada não fornece código nem aceita presença; participante sem inscrição confirmada não registra presença.
- **Fonte:** RN-302 e RN-306.

### P8 — Dados da presença
- **Pergunta:** Quais campos e origens devem aparecer no registro de presença?
- **Resposta:** O registro contém os campos do contrato. A origem é `qr`, `qr_offline` ou `manual`, conforme o modo de registro.
- **Fonte:** RN-315.

## Rodada 2

As respostas foram conferidas no documento de requisitos e registradas com as fontes RN-301 a RN-315.
