--- 
name: construir-telas 
description: Guia a construção das telas do frontend web em HTML e JS puro consumindo a API 
--- 

### Procedimento para Criação das Telas do M1 (Grade) 
1. **Estrutura Básica:** 
  * Crie os arquivos HTML e JS dentro da pasta `app/` (ex.: `index.html`, `detalhes.html`, `nova-atividade.html`, `app.js`). 

2. **Tela 1: Programação por Dia (M1)** 
  * Crie uma exibição ordenada por data/horário. 
  * Adicione um filtro `<select>` por tipo de atividade (Palestra, Minicurso). 
  * Consuma via `fetch('http://localhost:7000/api/atividades')`. 

3. **Tela 2: Detalhes da Atividade (M1)** 
  * Exiba nome, descrição, lista de encontros, salas e vagas restantes. 

4. **Tela 3: Formulário da Organização (M1)** 
  * Formulário para envio `POST`. Em caso de erro (status != 200/201), exiba o campo de erro retornado pela API visivelmente na tela.</select>