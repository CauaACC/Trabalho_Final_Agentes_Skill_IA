# Spec — M4: Certificados

## 1. Objetivo
Emitir certificados verificáveis para quem frequentou as atividades e disponibilizar o extrato de horas complementares do participante.

## 2. Fora de escopo
- Cadastro de atividades.
- Inscrições e lista de espera.
- Registro de presença.
- Painel analítico da organização.

## 3. Modelo
- Certificado: `codigo`, `atividadeId`, `participanteId`, `cargaHorariaMinutos`, `presencas`, `encontros`, `emitidoEm`.
- Verificação: `codigo`, `participante`, `atividade`, `cargaHorariaMinutos`, `emitidoEm`.
- Extrato: itens, totais por tipo, total bruto e aproveitado.

## 4. Endpoints
- `POST /atividades/:id/certificado` — participante.
- `GET /certificados` — participante.
- `GET /certificados/:codigo` — público.
- `GET /extrato` — participante.

## 5. Regras
- **R1 (P-01, RN-401, RN-402, RN-403):** certificado só é emitido a partir do fim do último encontro, não para atividade cancelada e somente para inscrição confirmada.
- **R2 (P-02, RN-404, RN-405):** frequência mínima é 75%, calculada por `presencas * 4 >= encontros * 3`; presença manual e offline contam normalmente.
- **R3 (P-05, RN-406, RN-407):** o certificado atesta a carga total da atividade; código `SA26-XXXX-XXXX` é único, permanente e repetição retorna 200.
- **R4 (P-06, RN-408, RN-409):** verificação pública aceita código minúsculo e mostra apenas nome abreviado, atividade, carga e data.
- **R5 (P-04, P-07, RN-410, RN-411, RN-412):** extrato lista toda atividade elegível, usa código `null` quando não emitida, limita palestras a 240 minutos e o aproveitado total a 1200 minutos.
- **R6 (P-01, P-02, P-03, RN-413):** a precedência é inexistente, cancelada, não inscrito, não encerrada e presença insuficiente; certificado já emitido retorna 200.

## 6. Critérios de aceite
- Primeira emissão retorna `201`; repetição retorna `200` com o mesmo código.
- Atividade aberta, cancelada, participante não inscrito e presença insuficiente retornam os erros do contrato.
- Participante lista somente os próprios certificados.
- Código válido pode ser verificado sem autenticação; código inexistente retorna `404`.
- Extrato informa itens, totais de palestras, minicursos, total e aproveitado, aplicando os dois tetos.

## 7. Como será verificado
Testes JUnit 5 em modo `MODO_TESTE=1`, mais testes de interface com fake da API.

## 8. Fatias
M4-R1 emissão, M4-R2 regras, M4-R3 listagem, M4-R4 verificação pública, M4-R5 extrato, M4-R6 interface.
