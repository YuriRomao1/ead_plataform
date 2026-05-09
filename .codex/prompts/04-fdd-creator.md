# 04 - FDD Creator

## Objetivo

Guiar a criação de um Feature Design Document para detalhar uma funcionalidade antes da implementação, conectando regras de negócio, contratos, eventos, persistência e testes esperados.

## Quando usar

Use este prompt antes de implementar uma funcionalidade de produto, endpoint, fluxo de domínio, publicação de evento ou consumo de evento.

## Entradas esperadas

- `docs/domain-context.md`.
- `docs/hld.md`.
- ADRs relacionados.
- Nome da funcionalidade.
- Objetivo da funcionalidade.
- Regras de negócio aplicáveis.
- Fluxos esperados.
- Integrações REST ou RabbitMQ, quando houver.

## Saída esperada

Um FDD em Markdown contendo:

- objetivo;
- escopo;
- fora de escopo;
- regras de negócio relacionadas;
- fluxo funcional;
- contratos esperados;
- eventos publicados ou consumidos;
- persistência envolvida;
- validações;
- cenários de erro;
- estratégia de testes;
- dúvidas e decisões pendentes.

## Regras

- Não implementar código.
- Não alterar arquitetura sem ADR.
- Não criar detalhes irrelevantes para a funcionalidade.
- Manter o FDD rastreável às regras de negócio.
- Explicitar o que está fora de escopo.
- Incluir testes esperados antes da implementação.

## Prompt de execução

```text
Você é um analista técnico criando um Feature Design Document para uma funcionalidade de uma plataforma Java com Spring Boot e microsserviços.

Leia o contexto de domínio, HLD, ADRs e a descrição da funcionalidade. Produza um FDD claro com objetivo, escopo, regras de negócio, fluxo funcional, contratos, eventos, persistência, validações, erros e estratégia de testes.

Não implemente código. Não altere arquitetura. Registre dúvidas e decisões pendentes quando necessário.
```
