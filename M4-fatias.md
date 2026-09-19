# M4 - Certificados

Cada fatia deve ser commitada separadamente com o nome do responsável.

## Fatia 1 - emissão do certificado
- Criar o armazenamento de certificados.
- Implementar `POST /atividades/:id/certificado`.
- Emitir certificado na primeira solicitação e retornar o mesmo certificado depois.
- Commit: `M4-R1: emite certificado da atividade`

## Fatia 2 - regras de emissão
- Validar participante inscrito.
- Recusar atividade cancelada ou ainda não encerrada.
- Validar presença suficiente nos encontros.
- Commit: `M4-R2: valida regras de emissao do certificado`

## Fatia 3 - certificados do participante
- Implementar `GET /certificados`.
- Retornar somente certificados do participante autenticado.
- Commit: `M4-R3: lista certificados do participante`

## Fatia 4 - verificação pública
- Implementar `GET /certificados/:codigo` sem autenticação.
- Retornar dados públicos do certificado.
- Commit: `M4-R4: verifica certificado publicamente`

## Fatia 5 - extrato de horas
- Implementar `GET /extrato`.
- Calcular itens, totais brutos e horas aproveitadas.
- Commit: `M4-R5: gera extrato de horas`

## Fatia 6 - interface web
- Criar tela de certificados e extrato.
- Criar consulta pública por código.
- Exibir erros amigáveis da API.
- Commit: `M4-R6: cria interface de certificados`