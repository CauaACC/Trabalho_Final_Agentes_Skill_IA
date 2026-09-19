# M3 - Presença por QR

## Divisão em fatias para commit individual

Cada fatia deve ficar em um commit separado. O autor do commit deve ser seu nome, para que fique registrado na história do repositório.

Exemplo:

```bash
git add .
git commit --author="Seu Nome <seu@email>" -m "M3-R1: gera código do encontro"
```

## Fatia 1 — geração do código do encontro
- Rota: `GET /encontros/:id/codigo`
- Regras: organização acessa, encontro existe, atividade não cancelada.
- Entrega mínima:
  - retorna `encontroId`, `codigo`, `trocaEm`, `validoAte`
  - código com 6 caracteres
  - mantém o contrato JSON

Commit sugerido:
- `M3-R1: gera codigo do encontro`

## Fatia 2 — validação do QR e presença online
- Rota: `POST /encontros/:id/presencas`
- Regras: participante inscrito, código válido, janela de presença, primeira presença cria registro, repetição retorna 200.
- Entrega mínima:
  - valida código do encontro
  - aceita `lidoEm` opcional
  - registra `origem: qr`
  - rejeita `CODIGO_INVALIDO`, `NAO_INSCRITO`, `FORA_DA_JANELA`

Commit sugerido:
- `M3-R2: valida QR e registra presenca online`

## Fatia 3 — sincronização tardia e presença offline
- Regras: leitura fora de rede, atraso de sincronização, `qr_offline`, `SINCRONIZACAO_TARDIA`.
- Entrega mínima:
  - aceita leitura offline quando ainda é válida para a regra de negócio
  - rejeita envio tardio fora da janela
  - usa `lidoEm` para a regra de tempo

Commit sugerido:
- `M3-R3: aceita leitura offline e rejeita sincronizacao tardia`

## Fatia 4 — presença manual
- Rota: `POST /encontros/:id/presencas/manual`
- Regras: organização registra presença, justificativa obrigatória, limite de manuais, participante inscrito.
- Entrega mínima:
  - payload com `participanteId` e `justificativa`
  - rejeita `JUSTIFICATIVA_OBRIGATORIA`, `NAO_INSCRITO`, `LIMITE_DE_MANUAIS`
  - registros com `origem: manual`

Commit sugerido:
- `M3-R4: presencia manual com justificativa`

## Fatia 5 — listagem de presenças
- Rota: `GET /encontros/:id/presencas`
- Regras: organização consulta todas as presenças do encontro.
- Entrega mínima:
  - retoma lista em ordem da criação
  - inclui `id`, `encontroId`, `participanteId`, `origem`, `lidoEm`, `registradaEm`, `justificativa`

Commit sugerido:
- `M3-R5: lista presencas do encontro`

## Fatia 6 — front-end M3
- tela da organização com QR em full screen e troca automática
- tela do participante para ler QR pela câmera ou digitar código
- leitura offline com envio posterior
- mensagem de erro amigável para falha da API

Commit sugerido:
- `M3-R6: interface de presenca por QR`

## Observação importante
- O commit ideal é 1 regra ou 1 fatia por commit.
- Não edite testes existentes para deixá-los verdes.
- Depois de cada commit, rode os testes do módulo M3 e só avance quando a fatia estiver estável.
