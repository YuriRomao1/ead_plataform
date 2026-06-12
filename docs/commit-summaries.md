# Commit Summaries

Este arquivo registra um resumo persistente e auditável dos commits do projeto.

Uso esperado:

- `commit`: resumo curto do que foi feito;
- `pull request`: contexto técnico completo para revisão;
- `docs/commit-summaries.md`: registro permanente do contexto principal de cada commit.

## Formato atual

Todo novo commit criado no projeto deve receber uma entrada neste arquivo usando o formato abaixo.

```md
## <commit-hash> - <commit-message>

### Changelog
What is this change about?

### Motivation
Why are we doing this change?

### Consequences
What are the advantages and disadvantages of making this change?
Does it impact other systems and/or applications?
Are there any risks?

### Metrics
How can we measure the success or failure of this change?

### Test Scenarios
How can we test this change?

### Evidence
How do we prove this change works?
```

## 0045ef4bc59e8e1dd411dc26977d21b2cc7618d1 - docs: add EAD workflow skills

### Changelog
Adiciona skills locais em `.codex/skills/` para revisão de código, governança de documentação, planejamento de features, mensageria/outbox, segurança/auth, execução de tarefas e engenharia de testes da EAD Platform.

Cada skill define contexto obrigatório, prioridades e critérios de saída para orientar agentes em tarefas específicas do projeto.

### Motivation
Padronizar o fluxo de trabalho assistido por IA para reduzir divergência entre implementação, documentação, testes, arquitetura e segurança.

As skills complementam a skill Java/Spring já existente sem alterar o comportamento da aplicação.

### Consequences
Advantages:
Melhora consistência de revisões, planejamento, execução e validação de tarefas no repositório.

Disadvantages:
As skills precisam ser mantidas sincronizadas com `AGENTS.md`, documentação arquitetural e práticas reais do projeto.

Impact:
Impacta somente documentação e configuração local de agentes. Não altera runtime, contratos HTTP, banco de dados, RabbitMQ ou build da aplicação.

Risks:
Se as skills ficarem desatualizadas, agentes podem seguir orientação antiga durante tarefas futuras.

### Metrics
- Existem skills locais para os principais tipos de tarefa do projeto.
- Cada skill declara contexto obrigatório e critérios de atuação.
- Nenhum arquivo de produção ou teste da aplicação é alterado nesse commit.

### Test Scenarios
- Revisar as novas skills em `.codex/skills/`.
- Confirmar que as instruções não contradizem `AGENTS.md`.
- Confirmar que os arquivos adicionados são documentação/configuração de agente.

### Evidence
- Revisão manual dos arquivos adicionados em `.codex/skills/`.
- `git show --stat --oneline 0045ef4bc59e8e1dd411dc26977d21b2cc7618d1`

## 3e08b79fe25fa9c6e2d4108266b603900fd8a003 - fix: pass correlation id to error responses

### Changelog
Ajusta a criação de `ApiErrorResponse` para receber o `correlationId` explicitamente a partir do `GlobalExceptionHandler`.

Remove a leitura direta de `CorrelationId.current()` de dentro do factory de resposta e mantém a montagem do erro concentrada no handler.

### Motivation
Deixar o contrato de erro mais explícito no ponto em que a resposta HTTP é construída.

Isso reduz acoplamento entre o DTO de erro e o contexto MDC usado para correlation id.

### Consequences
Advantages:
Torna a origem do `correlationId` mais clara no fluxo de tratamento de exceções.

Disadvantages:
Chamadas futuras ao factory de `ApiErrorResponse` precisarão informar o correlation id explicitamente.

Impact:
Impacta somente a montagem de respostas de erro no `auth-user-service`. O formato público da resposta permanece o mesmo.

Risks:
Baixo risco, desde que todos os pontos de criação do erro passem o correlation id correto.

### Metrics
- Respostas de erro continuam incluindo `correlationId`.
- O DTO `ApiErrorResponse` não lê mais o correlation id diretamente.
- Testes e build do módulo continuam passando.

### Test Scenarios
- Executar os testes do `auth-user-service`.
- Executar o build do `auth-user-service`.
- Revisar os usos de `ApiErrorResponse.of`.

### Evidence
- `.\gradlew.bat :auth-user-service:test`
- `.\gradlew.bat :auth-user-service:build`
- `git diff --check`
- `git show --stat --oneline 3e08b79fe25fa9c6e2d4108266b603900fd8a003`

## bf7ecbb43c4c1dbbcc9973d7823ac04b9f917c34 - chore: ignore local secret files

### Changelog
Atualiza `.gitignore` para ignorar arquivos locais de segredo e configuração sensível, incluindo `.env`, `.env.*`, `*.local.toml` e `*.local.json`, preservando `.env.example`.

### Motivation
Evitar que credenciais, tokens ou configurações locais sejam adicionados acidentalmente ao repositório.

### Consequences
Advantages:
Reduz risco de vazamento de segredos e mantém exemplos versionáveis via `.env.example`.

Disadvantages:
Arquivos locais ignorados não aparecerão no `git status`; desenvolvedores precisam versionar explicitamente exemplos seguros quando necessário.

Impact:
Impacta somente controle de versão. Não altera código, testes, banco de dados, RabbitMQ ou runtime.

Risks:
Baixo risco. Algum arquivo local que precise ser versionado deverá ser renomeado ou incluído como exceção explícita.

### Metrics
- Arquivos `.env` locais não aparecem como pendentes no Git.
- `.env.example` permanece elegível para versionamento.
- O commit altera apenas `.gitignore`.

### Test Scenarios
- Revisar as regras adicionadas em `.gitignore`.
- Confirmar que `.env.example` continua negado pela exceção.

### Evidence
- `git diff --check`
- `git show --stat --oneline bf7ecbb43c4c1dbbcc9973d7823ac04b9f917c34`

## bcaf74dfa23ff801a9b4a789adf0cb36c45c58cb - docs: add product requirements document

### Changelog
Adiciona `docs/PRD.md` como documento de requisitos de produto da EAD Platform.

O PRD documenta visão geral, problema, objetivos, personas, escopo do MVP, requisitos funcionais e não funcionais, critérios de sucesso, estado atual conhecido, riscos, dependências e relação com os documentos técnicos existentes.

### Motivation
Criar uma referência de produto acima do Domain Context, HLDs, ADRs, FDDs e planos de implementação.

O documento ajuda a orientar próximas funcionalidades sem misturar decisões de produto com detalhes técnicos de endpoints, tabelas, classes ou adapters.

### Consequences
Advantages:
Melhora rastreabilidade entre intenção de produto e documentação técnica.

Disadvantages:
O PRD precisa ser mantido sincronizado quando o escopo do MVP ou o estado implementado mudarem.

Impact:
Impacta somente documentação. Não altera código, contratos HTTP, banco de dados, RabbitMQ, testes ou runtime.

Risks:
Se futuras funcionalidades forem implementadas sem atualizar PRD, Domain Context, HLD, FDDs e planos, a documentação pode divergir do produto real.

### Metrics
- Existe um PRD em `docs/PRD.md`.
- O PRD diferencia escopo implementado, planejado e fora de escopo inicial.
- O PRD referencia os documentos técnicos usados como fonte de rastreabilidade.
- O commit altera apenas documentação de produto.

### Test Scenarios
- Revisar `docs/PRD.md` contra `docs/domain-context.md`.
- Revisar `docs/PRD.md` contra `docs/hld.md`.
- Confirmar que o PRD não declara `course-service` ou `notification-service` como implementados.
- Confirmar que o PRD não detalha implementação técnica que pertence a FDDs, HLDs ou planos.

### Evidence
- Revisão manual de `docs/PRD.md`.
- `git diff --check -- docs\PRD.md`
- `git show --stat --oneline bcaf74dfa23ff801a9b4a789adf0cb36c45c58cb`

## Histórico legado

As entradas abaixo foram registradas antes da adoção do formato estruturado atual e são mantidas apenas como histórico resumido.

## 9cdc1fafa133db095ebbb217590a9361d79ee31a - feat: add request correlation id

### Changelog
Adds HTTP request correlation id support to `auth-user-service`.

Introduces `CorrelationId` and `CorrelationIdFilter` to reuse incoming `X-Correlation-Id` values, generate UUIDs when absent, write the value to MDC, return it in response headers, and clean MDC after each request.

Extends error responses with `correlationId` and configures logging to include the MDC value.

Updates controller tests and Auth/User documentation for the new correlation behavior.

### Motivation
Operational troubleshooting needs a stable identifier that ties HTTP responses, application logs, and error bodies together.

This change implements the observability requirement already described in the HLD and prepares the service for future cross-service tracing.

### Consequences
Advantages:
Improves request traceability and makes error responses easier to correlate with logs without exposing sensitive data.

Disadvantages:
The public error response shape changes by adding `correlationId`, so clients parsing the full body must tolerate the new field.

Impact:
Impacts only HTTP request/response behavior, log formatting, and documentation in `auth-user-service`.

Risks:
Correlation id is currently limited to HTTP logs and error responses; event payload propagation remains out of scope for this task.

### Metrics
- Responses include `X-Correlation-Id`.
- Requests with `X-Correlation-Id` receive the same value back.
- Requests without `X-Correlation-Id` receive a generated UUID.
- Error bodies include `correlationId`.
- MDC is cleared after each request.
- Module tests and build remain green.

### Test Scenarios
- Create a user without `X-Correlation-Id` and verify a UUID response header.
- Create a user with `X-Correlation-Id` and verify the same value is returned.
- Create a user with blank `X-Correlation-Id` and verify a UUID is generated.
- Trigger duplicate email handling and verify `correlationId` appears in the error body.
- Verify MDC is cleared after request processing.

### Evidence
- `.\gradlew.bat :auth-user-service:test --tests "com.yuriromao.ead.authuser.infrastructure.web.UserControllerTest"`
- `.\gradlew.bat :auth-user-service:test`
- `.\gradlew.bat :auth-user-service:build`
- `git diff --check`

## 5261d63c2128188f5935024bc1be40e27e13b5c8 - fix: map duplicate email constraint to conflict

### Changelog
Maps database duplicate-email constraint violations in `JpaUserRepository` to `UserEmailAlreadyExistsException`.

Uses `saveAndFlush` so the `users.email` unique constraint is evaluated inside the persistence adapter and preserves the original database exception as the cause.

Adds persistence coverage for the duplicate-email mapping and for rethrowing unrelated integrity violations.

Documents T23 in the Auth/User implementation plan and removes the completed HLD next step.

### Motivation
The application-level `existsByEmail` check covers the common duplicate email path, but concurrent requests can still pass that check before either transaction commits.

The database unique constraint is the final protection, so its duplicate-email violation must produce the same public `409 Conflict` contract instead of leaking as an internal error.

### Consequences
Advantages:
Keeps the HTTP contract stable under race conditions and preserves diagnostic context by retaining the original persistence exception as the cause.

Disadvantages:
The mapping depends on the PostgreSQL-generated constraint name `users_email_key`; if the migration is renamed later, the adapter check must be updated.

Impact:
Impacts only `auth-user-service` user persistence behavior and related tests/documentation.

Risks:
Unexpected duplicate-email constraint naming changes would cause the persistence exception to be rethrown instead of mapped to `UserEmailAlreadyExistsException`.

### Metrics
- Duplicate `users.email` database violations are mapped to `UserEmailAlreadyExistsException`.
- `UserEmailAlreadyExistsException` continues to map to HTTP `409 Conflict`.
- Non-email integrity violations are not masked as duplicate email.
- Module tests and build remain green.

### Test Scenarios
- Save two users with the same email through `JpaUserRepository`.
- Verify the second save throws `UserEmailAlreadyExistsException`.
- Verify the domain exception preserves the original cause.
- Simulate a non-email `DataIntegrityViolationException` and verify it is rethrown.
- Verify the existing controller test still maps duplicate email to `409 Conflict`.

### Evidence
- `.\gradlew.bat :auth-user-service:test --tests "com.yuriromao.ead.authuser.infrastructure.persistence.JpaUserRepositoryTest"`
- `.\gradlew.bat :auth-user-service:build`
- `git diff --check`

## 322c8714ee5476e6ca5aef971a8b2bfb0c0960ff - feat: restrict public user registration roles

### Changelog
Restricts public `POST /users` registration to the `STUDENT` role only.

Adds a dedicated business exception for non-public roles, maps it to HTTP `400 Bad Request` with code `USER_ROLE_NOT_ALLOWED_FOR_PUBLIC_REGISTRATION`, and updates application and controller tests.

Updates the Auth/User FDD, HLDs, implementation plan, and service README to reflect the implemented public-registration rule.

### Motivation
Public registration must not allow users to self-assign elevated roles such as `TEACHER` or `ADMIN`.

The change aligns the implementation with the documented Auth/User scope and keeps elevated-role creation reserved for a future protected administrative flow.

### Consequences
Advantages:
Reduces authorization risk in the public user creation endpoint and makes the current role policy explicit in code, tests, and documentation.

Disadvantages:
There is still no administrative endpoint for creating `TEACHER` or `ADMIN` users, so those roles remain domain values without a supported creation flow.

Impact:
Impacts only `auth-user-service` public registration behavior and related documentation.

Risks:
Existing clients that attempted to create non-`STUDENT` users through `POST /users` now receive `400 Bad Request`.

### Metrics
- `POST /users` accepts requests with only `STUDENT`.
- `POST /users` rejects `TEACHER`, `ADMIN`, and mixed role sets containing non-public roles.
- Role rejection does not persist a user.
- Role rejection does not record a `UserCreated` outbox event.
- Module tests and build remain green.

### Test Scenarios
- Create a user with `STUDENT`.
- Reject public registration with `TEACHER`.
- Reject public registration with `ADMIN`.
- Reject public registration with multiple roles when any role is not allowed.
- Verify HTTP error code `USER_ROLE_NOT_ALLOWED_FOR_PUBLIC_REGISTRATION`.
- Verify invalid-role flows do not save a user or record an outbox event.

### Evidence
- `.\gradlew.bat :auth-user-service:test`
- `.\gradlew.bat :auth-user-service:build`
- `git diff --check`

## 955b251 - feat: add outbox event persistence

### Changelog
Adds JPA-based persistence for domain events in the `outbox_events` table of `auth-user-service`.

### Motivation
Implements the local persistence layer required by ADR-006 so domain events can be recorded in the same database transaction as local state changes.

### Consequences
Advantages:
Creates a durable local record of pending domain events and removes the need to depend on immediate RabbitMQ availability during event recording.

Disadvantages:
Adds persistence complexity and temporarily keeps both the legacy JDBC recorder class and the new JPA-based adapter in the module.

Impact:
Impacts only `auth-user-service` and only its own database structures and application persistence boundary.

Risks:
If multiple recorder implementations become active in the Spring context at the same time, the application could become ambiguous. This change avoids that by making the JPA adapter the active bean and removing the old recorder from the Spring context.

### Metrics
- New `UserCreated` events are inserted into `outbox_events`.
- New rows start with `status = PENDING`.
- New rows start with `attempts = 0`.
- Module test and build success rate remains green.

### Test Scenarios
- Persist a valid `UserCreatedEvent` in `outbox_events`.
- Verify `eventId`, `eventType`, `aggregateType`, and `aggregateId`.
- Verify initial `PENDING` status and `attempts = 0`.
- Verify payload does not contain password or hash.
- Verify the recorder fails when the event is null.

### Evidence
- `.\gradlew.bat :auth-user-service:test --tests "com.yuriromao.ead.authuser.infrastructure.outbox.JpaDomainEventRecorderTest"`
- `.\gradlew.bat :auth-user-service:test`
- `.\gradlew.bat :auth-user-service:build`

## b874bb1 - docs: add outbox implementation tasks

### Changelog
Updates the implementation plan and HLDs to formalize T16, T17, T18, and T19 for the transactional outbox flow.

### Motivation
Brings the project documentation in line with ADR-006 and ADR-007 so the outbox work is explicitly planned, traceable, and reviewable.

### Consequences
Advantages:
Clarifies the intended sequence of outbox persistence, event recording, async publishing, and test coverage.

Disadvantages:
Adds more process detail to the implementation plan and requires future tasks to keep documentation and code aligned.

Impact:
Impacts project documentation only. No runtime behavior or production code changed in this commit.

Risks:
If future code evolves without updating the plan and HLDs, the documentation can drift again from the real implementation state.

### Metrics
- T16 to T19 are explicitly documented in the implementation plan.
- HLD documents the outbox as the local source of pending events.
- HLD documents producer retry/status flow and consumer idempotency expectations.

### Test Scenarios
- Review the updated plan to confirm T16, T17, T18, and T19 include scope, acceptance criteria, validation commands, and suggested commit messages.
- Review HLD-001 to confirm `auth-user-service` records events before publishing.
- Review HLD-004 to confirm pending events, async relay, retry, statuses, duplication risk, and consumer idempotency are covered.

### Evidence
- Manual review of:
  - `docs/implementation-plans/plan-001-auth-user-service.md`
  - `docs/hlds/hld-001-auth-user-service.md`
  - `docs/hlds/hld-004-event-driven-communication.md`

## 2245753 - docs: align outbox documentation details

### Changelog
Atualiza READMEs, FDD-001, ADR-006, HLD global e HLD-004 para remover linguagem desatualizada sobre Outbox e RabbitMQ.

### Motivation
Manter a documentação alinhada ao estado real do `auth-user-service`, que já registra `UserCreated` na Transactional Outbox e publica eventos pendentes de forma assíncrona.

### Consequences
Advantages:
Reduz ambiguidade para próximas tarefas e deixa claro que ADR-007 já define a topologia inicial de mensageria.

Disadvantages:
Não remove código legado de Outbox JDBC; essa limpeza continua como tarefa técnica separada.

Impact:
Impacta somente documentação. Não altera runtime, testes, migrations ou configuração.

Risks:
Se o código legado de Outbox JDBC permanecer por muito tempo, novos contribuidores podem confundir o fluxo ativo com o fluxo antigo.

### Metrics
- Documentos não contêm mais os textos obsoletos identificados na auditoria.
- READMEs descrevem `UserCreated` como gravado na Outbox e publicado assincronamente.
- FDD e HLD referenciam ADR-007 como decisão existente.

### Test Scenarios
- Revisar os documentos alterados para confirmar alinhamento com o fluxo atual de Outbox.
- Buscar termos antigos sobre RabbitMQ não formalizado, implementação futura da Outbox e publicação direta simplificada.

### Evidence
- `rg` para confirmar remoção dos textos obsoletos.
- `git diff --check`

## 1da83b0 - docs: improve code documentation

### Changelog
Adiciona Javadocs curtos em classes e métodos públicos relevantes do `auth-user-service`, cobrindo domínio, aplicação, ports, adapters HTTP, persistência, segurança, RabbitMQ e Outbox.

### Motivation
Melhorar a legibilidade do código para novos desenvolvedores sem violar a regra do projeto de manter código e comentários Java em inglês.

### Consequences
Advantages:
Facilita onboarding e revisão de responsabilidades entre camadas sem alterar comportamento.

Disadvantages:
Comentários precisam continuar sincronizados quando responsabilidades mudarem.

Impact:
Impacta somente documentação inline no código Java. Não altera contrato HTTP, banco, migrations, configuração ou lógica de runtime.

Risks:
Comentários excessivos podem ficar obsoletos se mudanças futuras não atualizarem a documentação junto com o código.

### Metrics
- Classes e métodos públicos centrais possuem Javadocs de responsabilidade.
- Build do módulo continua passando.
- Formatação Java continua válida pelo Spotless.

### Test Scenarios
- Executar validação de formatação Java.
- Executar build completo do `auth-user-service`.
- Revisar se os comentários explicam fronteiras de domínio, aplicação e infraestrutura sem duplicar código trivial.

### Evidence
- `.\gradlew.bat :auth-user-service:spotlessJavaCheck`
- `.\gradlew.bat :auth-user-service:build`
- `git diff --check`

| Commit | Mensagem | Mini resumo |
| --- | --- | --- |
| `8416c7c` | `first commit` | Inicia o repositório com a base inicial do projeto. |
| `13a3c88` | `docs: add initial domain context` | Documenta o contexto de domínio inicial da EAD Platform. |
| `6cd5619` | `docs: add AI agent project guidelines` | Adiciona regras de trabalho para agentes de IA no projeto. |
| `9ec2cfb` | `docs: add high level architecture design` | Cria o desenho arquitetural de alto nível da plataforma. |
| `e3b62e6` | `docs: record microservices database per service decision` | Registra a decisão arquitetural de database per service. |
| `e906a6b` | `chore: add local infrastructure with postgres and rabbitmq` | Adiciona infraestrutura local com PostgreSQL e RabbitMQ. |
| `a72719b` | `docs: add project readme` | Cria a documentação inicial de uso do repositório. |
| `f23fad6` | `add AI workflow prompts and auth user FDD` | Adiciona prompts de workflow e o primeiro FDD do auth-user-service. |
| `9e55f15` | `update auth user service FDD` | Refina o FDD de criação de usuários. |
| `90ee46b` | `add password hashing strategy ADR` | Registra a estratégia inicial de hash de senha. |
| `f740389` | `update password hashing strategy ADR` | Ajusta a ADR de hash de senha. |
| `7204bed` | `docs: add auth user service implementation plan` | Cria o plano técnico de implementação do auth-user-service. |
| `e6870df` | `docs: update Java and Spring Boot baseline` | Atualiza a baseline técnica para Java, Spring Boot e Gradle. |
| `96ebce8` | `build: move application to auth user service module` | Move a aplicação Spring Boot para o módulo `auth-user-service`. |
| `01bff51` | `docs: add project-specific Java Spring microservices skill` | Adiciona a skill específica do projeto para Java, Spring e microservices. |
| `ddb4550` | `docs: update high-level design` | Atualiza o HLD global com o estado arquitetural mais recente. |
| `7302d0f` | `docs: add HLD reviewer prompt` | Adiciona prompt de revisão para documentos HLD. |
| `c4d32c7` | `docs: add component HLDs` | Cria HLDs específicos para os componentes iniciais. |
| `88f74fb` | `docs: record testing strategy` | Registra a estratégia de testes do projeto. |
| `065fcbf` | `config: add auth user service database settings` | Configura datasource e perfil de testes do auth-user-service. |
| `14e050c` | `db: add auth user schema migration` | Cria a migration inicial de usuários e papéis. |
| `7f151e5` | `feat: add auth user domain model` | Adiciona o modelo de domínio de usuário. |
| `5c48cd4` | `feat: add auth user domain model` | Complementa o modelo de domínio e seus testes. |
| `fbce949` | `feat: add auth user repository` | Implementa a porta e o adapter JPA de persistência de usuários. |
| `adcac7d` | `feat: add testing agent documentation` | Adiciona documentação de apoio para agentes de teste. |
| `8c39ace` | `feat: add bcrypt password hashing` | Implementa hash de senha com BCrypt. |
| `461fe86` | `feat: add create user use case` | Cria o caso de uso de cadastro de usuário. |
| `2ee5a80` | `feat: add create user endpoint` | Expõe o endpoint HTTP `POST /users`. |
| `28f70de` | `feat: add user created event model` | Adiciona o modelo do evento `UserCreated`. |
| `8733c57` | `feat: configure rabbitmq event publisher` | Configura o publisher RabbitMQ para eventos de domínio. |
| `ff48195` | `feat: publish user created event` | Integra criação de usuário com publicação direta de `UserCreated`. |
| `5443eae` | `test: cover auth user creation flow` | Consolida testes do fluxo de criação de usuário. |
| `1dfd213` | `docs: document auth user service` | Documenta execução e validação do auth-user-service. |
| `3e0f518` | `docs: record transactional outbox strategy` | Registra a decisão de usar Transactional Outbox Pattern. |
| `05cc174` | `feat: add outbox events migration` | Cria a tabela `outbox_events` e valida sua migration. |
| `f0f7ce5` | `feat: record user created events in outbox` | Troca a publicação direta no use case por gravação transacional na outbox. |
| `b8b75e1` | `feat: publish pending outbox events` | Adiciona relay assíncrono para publicar eventos pendentes da outbox no RabbitMQ com controle de status e tentativas. |
| `1f7c5ae` | `test: cover outbox event publishing` | Complementa a cobertura da outbox com proteção contra acoplamento direto ao publisher e validação real de unicidade de `event_id`. |
| `439fef5` | `ci: add pull request workflow` | Adiciona workflow de PR com Java 25, Gradle build, verificação de testes/cobertura e upload de artifacts. |
| `84f925d` | `docs: align HLD with implemented outbox flow` | Alinha HLDs e plano de implementação ao estado real do auth-user-service, incluindo Outbox e publisher assíncrono implementados. |
