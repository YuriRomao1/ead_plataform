# 05 - Implementation Plan Creator

## Objetivo

Guiar a criação de um plano de implementação técnico e executável para uma funcionalidade já documentada em FDD.

## Quando usar

Use este prompt depois da criação do FDD e antes de iniciar alterações no código.

## Entradas esperadas

- `docs/domain-context.md`.
- `docs/hld.md`.
- ADRs relacionados.
- FDD da funcionalidade.
- Estrutura atual do repositório.
- Restrições técnicas conhecidas.

## Saída esperada

Um plano de implementação em Markdown contendo:

- objetivo técnico;
- pré-requisitos;
- arquivos ou módulos esperados;
- sequência de tarefas;
- contratos a criar ou alterar;
- regras de domínio a implementar;
- persistência e migrações, quando aplicável;
- mensageria, quando aplicável;
- testes necessários;
- critérios de aceite;
- riscos e observações.

## Regras

- Não implementar código durante a criação do plano.
- Não incluir tarefas fora do escopo do FDD.
- Não propor refatorações não relacionadas.
- Separar tarefas pequenas e verificáveis.
- Incluir testes como parte obrigatória do plano.
- Indicar quando documentação ou ADR precisa ser atualizada.

## Prompt de execução

```text
Você é um engenheiro de software criando um plano de implementação para uma funcionalidade já descrita em FDD.

Leia o contexto de domínio, HLD, ADRs, FDD e estrutura atual do repositório. Produza um plano técnico passo a passo, com arquivos ou módulos prováveis, sequência de tarefas, persistência, mensageria, testes, critérios de aceite e riscos.

Não implemente código neste momento. Não adicione escopo que não esteja no FDD.
```
