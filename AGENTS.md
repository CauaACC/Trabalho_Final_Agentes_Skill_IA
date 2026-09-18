# AGENTS.md — Semana Acadêmica (raiz)

## Stack
- Linguagem: Java 21
- Build: Maven
- Framework HTTP: Javalin (leve, sem auto-configuração — decisões explícitas)
- Banco: SQLite via sqlite-jdbc (arquivo local em `.\database.db`, sem servidor)
- Interface: Web (HTML + JavaScript puro, fetch para consumir a API)
- Testes API: JUnit 5
- Ambiente de Execução: Windows (PowerShell / CMD)

## Estrutura do repositório
- api\ — API Java (Javalin) — tem seu próprio AGENTS.md
- app\ — Interface web — tem seu próprio AGENTS.md
- entrevistas\, specs\, auditorias\ — Documentação do processo (SDD)

## Convenções
- Um commit por regra ou fatia da spec, citando o código da regra
  (ex.: "M1-R3: recusa sobreposição de encontros")
- Nunca editar um teste existente só para fazê-lo passar
- Alteração neste arquivo explica na mensagem do commit por que mudou
- Scripts de build e comandos de terminal devem usar sintaxe compatível com Windows (`mvnw.cmd`, caminhos com `\`)

## Aprendizados (adicionar só quando o agente errar de verdade)
- (vazio por enquanto)