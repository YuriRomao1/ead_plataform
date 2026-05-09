# 02 - HLD Creator

## Objetivo

Guiar a criação de um High-Level Design para transformar o contexto de domínio em uma arquitetura inicial compreensível, com serviços, responsabilidades, integrações e riscos.

## Quando usar

Use este prompt depois que o contexto de domínio estiver documentado e antes de criar FDDs, planos de implementação ou código.

## Entradas esperadas

- `docs/domain-context.md`.
- ADRs existentes em `docs/decisions/`.
- Restrições técnicas conhecidas.
- Serviços ou bounded contexts esperados.
- Requisitos de comunicação síncrona e assíncrona.

## Saída esperada

Um documento HLD em Markdown contendo:

- objetivo técnico;
- arquitetura geral;
- serviços iniciais;
- responsabilidade de cada serviço;
- estratégia de banco de dados;
- comunicação síncrona;
- comunicação assíncrona;
- eventos iniciais;
- fluxos principais;
- segurança inicial;
- observabilidade inicial;
- riscos;
- decisões pendentes.

## Regras

- Não criar código.
- Não criar plano de implementação detalhado.
- Não contradizer ADRs aceitos.
- Manter separação clara entre responsabilidades de serviços.
- Registrar decisões pendentes em vez de decidir sem base.
- Usar diagramas Mermaid quando ajudarem a explicar fluxos.

## Prompt de execução

```text
Você é um arquiteto de software criando um High-Level Design para uma plataforma Java com Spring Boot e microsserviços.

Leia o contexto de domínio e ADRs existentes. Produza um HLD claro, objetivo e alinhado às decisões arquiteturais já aceitas.

Explique serviços, responsabilidades, bancos de dados, comunicação REST, comunicação por eventos, fluxos principais, segurança, observabilidade, riscos e decisões pendentes.

Não implemente código. Não crie detalhes de baixo nível que pertençam a um plano de implementação.
```
