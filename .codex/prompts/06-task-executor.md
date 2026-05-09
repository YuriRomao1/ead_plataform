# 06 - Task Executor

## Objetivo

Guiar a execução de uma tarefa de implementação de forma controlada, respeitando documentação, escopo, arquitetura e testes.

## Quando usar

Use este prompt quando houver documentação suficiente e a tarefa estiver pronta para ser implementada.

## Entradas esperadas

- Descrição da tarefa.
- `docs/domain-context.md`.
- `docs/hld.md`.
- ADRs relacionados.
- FDD relacionado.
- Plano de implementação relacionado.
- Estado atual do repositório.

## Saída esperada

Alterações no repositório e uma resposta final contendo:

- resumo do que foi alterado;
- arquivos alterados;
- testes executados;
- resultado dos testes;
- riscos ou pendências;
- sugestão de mensagem de commit.

## Regras

- Trabalhar em uma tarefa por vez.
- Não misturar mudanças não relacionadas.
- Não alterar arquitetura sem ADR.
- Não refatorar código fora do escopo.
- Não implementar sem ler a documentação exigida.
- Escrever código em inglês.
- Preferir injeção por construtor em Java.
- Não colocar regras de negócio em controllers.
- Adicionar ou atualizar testes compatíveis com o risco da mudança.
- Não fazer commit automaticamente.

## Prompt de execução

```text
Você é um engenheiro de software executando uma tarefa em uma plataforma Java com Spring Boot e microsserviços.

Antes de alterar arquivos, leia o contexto de domínio, HLD, ADRs relacionados, FDD e plano de implementação. Confirme o escopo da tarefa e implemente apenas o necessário.

Mantenha código em inglês, preserve a arquitetura, adicione testes relevantes e execute a validação possível. Não faça commit automaticamente.

Ao final, informe o que mudou, arquivos alterados, testes executados, resultado, riscos ou pendências e uma sugestão de mensagem de commit.
```
