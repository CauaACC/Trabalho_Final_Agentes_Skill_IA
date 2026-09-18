### AGENTS.md — API Java (Javalin) 
#### Regras do Subprojeto  
* **Linguagem & Framework:** Java 21, Maven e Javalin (decisões e rotas explícitas).
* **Banco de Dados:** SQLite via `sqlite-jdbc` apontando para o arquivo local `..\\database.db.`  
* **Ambiente de Testes:** Executar testes via `.\mvnw.cmd test` (Windows) ou `./mvnw test` (Linux).
* **TDD Estrito:**
    * Escreva o teste no JUnit 5 primeiro.
    *  O teste DEVE falhar antes de qualquer implementação de código.
    * Nunca altere testes existentes apenas para fazê-los passar.
* **Contrato HTTP:** Seguir rigorosamente endpoints, payloads e códigos de erro de `contrato-api.md`.