# 01 - Domain Context

## Objetivo

Guiar a criação ou evolução do contexto de domínio do projeto, registrando visão geral, personas, bounded contexts, entidades, regras de negócio e eventos de domínio.

## Quando usar

Use este prompt no início do projeto ou quando uma nova área de negócio precisar ser entendida antes de decisões técnicas, FDDs ou implementação.

## Entradas esperadas

- Descrição do produto ou problema de negócio.
- Personas ou tipos de usuários envolvidos.
- Processos principais do domínio.
- Regras de negócio conhecidas.
- Eventos ou integrações esperadas.
- Documentos existentes, quando houver.

## Saída esperada

Um documento de contexto de domínio em Markdown contendo:

- visão geral;
- personas;
- bounded contexts;
- entidades principais;
- regras de negócio numeradas;
- eventos de domínio;
- dúvidas e decisões pendentes.

## Regras

- Não criar código.
- Não definir detalhes de infraestrutura.
- Não tomar decisões arquiteturais sem explicitar premissas.
- Usar linguagem clara e orientada ao negócio.
- Manter regras de negócio identificáveis e rastreáveis.
- Preservar decisões já registradas em ADRs existentes.

## Prompt de execução

```text
Você é um analista de domínio ajudando a documentar o contexto de uma plataforma Java com arquitetura de microsserviços.

Leia as entradas fornecidas e produza ou atualize um documento de contexto de domínio.

O documento deve explicar o negócio antes da tecnologia. Identifique personas, bounded contexts, entidades principais, regras de negócio numeradas e eventos de domínio.

Não implemente código. Não proponha infraestrutura. Quando houver incertezas, registre como dúvidas ou decisões pendentes.
```
