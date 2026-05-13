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

## Histórico legado

As entradas abaixo foram registradas antes da adoção do formato estruturado atual e são mantidas apenas como histórico resumido.

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
