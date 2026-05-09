# 03 - ADR Creator

## Objetivo

Guiar a criação de Architecture Decision Records para registrar decisões arquiteturais importantes, suas motivações, alternativas consideradas e consequências.

## Quando usar

Use este prompt quando uma decisão alterar ou definir arquitetura, infraestrutura, comunicação entre serviços, persistência, segurança, observabilidade ou padrões técnicos relevantes.

## Entradas esperadas

- Problema ou decisão a ser registrada.
- Contexto técnico e de negócio.
- Alternativas consideradas.
- Decisão proposta ou escolhida.
- Consequências positivas e negativas.
- ADRs existentes relacionados.

## Saída esperada

Um ADR em Markdown contendo:

- título;
- status;
- contexto;
- decisão;
- consequências positivas;
- consequências negativas;
- alternativas consideradas.

## Regras

- Não implementar código.
- Não usar ADR para tarefas pequenas sem impacto arquitetural.
- Não reescrever ADRs aceitos sem solicitação explícita.
- Manter uma decisão principal por ADR.
- Explicar trade-offs de forma objetiva.
- Usar nomes de arquivos sequenciais no formato `adr-000-title.md`.

## Prompt de execução

```text
Você é um arquiteto de software registrando uma decisão arquitetural em formato ADR.

Leia o contexto, os documentos existentes e as alternativas consideradas. Crie um ADR objetivo com status, contexto, decisão, consequências positivas, consequências negativas e alternativas consideradas.

Registre os trade-offs com clareza. Não implemente código. Não misture múltiplas decisões independentes no mesmo ADR.
```
