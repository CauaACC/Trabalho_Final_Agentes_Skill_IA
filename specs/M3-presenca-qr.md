# Spec — M3: Presença por QR

## 1. Objetivo
Registrar presença nos encontros da Semana Acadêmica por QR, incluindo leitura offline e registro manual controlado pela organização.

## 2. Fora de escopo
- Inscrições e lista de espera.
- Emissão de certificados.
- Painel analítico da organização.

## 3. Modelo
- Código do encontro: `encontroId`, `codigo`, `trocaEm`, `validoAte`.
- Presença: `id`, `encontroId`, `participanteId`, `origem`, `lidoEm`, `registradaEm`, `justificativa`.

## 4. Endpoints
- `GET /encontros/:id/codigo` — organização.
- `POST /encontros/:id/presencas` — participante.
- `POST /encontros/:id/presencas/manual` — organização.
- `GET /encontros/:id/presencas` — organização.

## 5. Regras
- **R1 (P-01, RN-303, RN-304):** o código muda a cada minuto; o código atual e o do minuto anterior são aceitos, com 6 caracteres do alfabeto sem `0`, `O`, `1` e `I`, aceitando minúsculas e espaços na leitura.
- **R2 (P-02, RN-301, RN-302, RN-306):** a janela vai de 15 minutos antes a 30 minutos depois do início, com bordas incluídas; atividade cancelada não fornece código e só participante confirmado registra presença.
- **R3 (P-03, RN-308, RN-309, RN-310):** `lidoEm` é o instante da leitura; leitura posterior ao envio usa o instante do envio; sincronização é aceita até 2 horas após o fim.
- **R4 (P-04, RN-307):** presença é única por participante e encontro; repetição devolve 200 com o mesmo registro antes das demais regras.
- **R5 (P-06, RN-311, RN-312, RN-313):** presença manual exige organização, participante confirmado, justificativa de pelo menos 10 caracteres, janela própria e limite de 10% das confirmações, arredondado para cima.
- **R6 (P-07, P-08, RN-314, RN-315):** a precedência de erros e as origens `qr`, `qr_offline` e `manual` seguem o contrato.

## 6. Critérios de aceite
- Código válido retorna os campos do contrato.
- QR online retorna `201` e repetição retorna `200`.
- QR inválido, não inscrição e janela inválida retornam os códigos do contrato.
- Leitura offline válida retorna `qr_offline`; sincronização tardia retorna `SINCRONIZACAO_TARDIA`.
- Manual com justificativa menor que 10 caracteres e manual acima do limite são recusados.
- Organização consulta a lista completa; participante não consulta a lista.

## 7. Como será verificado
Testes JUnit 5 em modo `MODO_TESTE=1`, com relógio controlado e banco resetado. Testes de interface devem usar fake da API.

## 8. Fatias
M3-R1 código, M3-R2 QR online, M3-R3 offline, M3-R4 manual, M3-R5 listagem, M3-R6 interface.
