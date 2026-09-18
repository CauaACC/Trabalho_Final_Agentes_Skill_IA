### AGENTS.md — Interface Web (Frontend)

#### Regras do Subprojeto
* **Tecnologia:** HTML5 e JavaScript puro (ES6+), sem frameworks.
* **Comunicação:** Consumir a API Java via `fetch()`.
* **Tratamento de Erros:** Exibir mensagens de erro amigáveis na tela ao receber falhas da API.
* **Módulo M1 — Telas Obrigatórias:**
  1. **Programação por dia:** Exibição da grade com filtro por tipo de atividade.
  2. **Detalhes da atividade:** Exibição dos encontros, salas e vagas disponíveis.
  3. **Formulário da organização:** Cadastro de atividades exibindo os erros da API.
* **Testes de Interface:** Utilizar fakes/mocks para simular os retornos da API.