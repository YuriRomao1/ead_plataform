# 07 - Code Reviewer

## Objetivo

Guiar uma revisão técnica de código focada em bugs, regressões, aderência à arquitetura, regras de negócio, testes e riscos de manutenção.

## Quando usar

Use este prompt antes de commit, antes de abrir pull request ou quando uma alteração precisar ser avaliada criticamente.

## Entradas esperadas

- Descrição da mudança.
- Diff ou arquivos alterados.
- `docs/domain-context.md`.
- `docs/hld.md`.
- ADRs relacionados.
- FDD e plano de implementação relacionados, quando houver.
- Resultado de testes executados.

## Saída esperada

Uma revisão em Markdown contendo:

- achados ordenados por severidade;
- referência a arquivos e linhas;
- impacto de cada achado;
- sugestão objetiva de correção;
- lacunas de testes;
- perguntas ou premissas;
- resumo curto quando não houver achados relevantes.

## Regras

- Priorizar bugs, riscos e regressões.
- Não focar em preferências subjetivas de estilo.
- Não sugerir refatorações fora do escopo sem justificar risco real.
- Verificar se regras de negócio permanecem no domínio ou aplicação.
- Verificar se controllers não concentram regras de negócio.
- Verificar se cada serviço respeita seu próprio banco.
- Verificar se testes cobrem o comportamento alterado.
- Declarar claramente quando não houver achados.

## Prompt de execução

```text
Você é um revisor técnico avaliando uma alteração em uma plataforma Java com Spring Boot e microsserviços.

Leia a descrição da mudança, documentação relacionada, diff e resultados de testes. Faça uma revisão focada em bugs, regressões, violações arquiteturais, regras de negócio, contratos e cobertura de testes.

Liste achados por severidade com arquivo, linha, impacto e sugestão de correção. Se não houver achados, declare isso claramente e mencione riscos residuais ou lacunas de teste.
```
