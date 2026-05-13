# Plan-001: Auth/User Service Implementation Plan

## Contexto

Este plano transforma o FDD `docs/fdds/fdd-001-auth-user-service.md` em tarefas pequenas, ordenadas por dependência técnica, para implementar a primeira entrega do `auth-user-service`.

Documentos de referência:

- `AGENTS.md`
- `docs/fdds/fdd-001-auth-user-service.md`
- `docs/decisions/adr-001-microservices-database-per-service.md`
- `docs/decisions/adr-002-password-hashing-strategy.md`
- `docs/decisions/adr-006-transactional-outbox-for-domain-events.md`
- `docs/decisions/adr-007-rabbitmq-topology-and-retry-dlq-strategy.md`

Escopo da entrega:

- criar usuário;
- validar nome, e-mail, senha e papéis;
- garantir e-mail único;
- salvar senha somente como hash BCrypt;
- usar papéis `STUDENT`, `TEACHER` e `ADMIN`;
- usar status `ACTIVE` e `BLOCKED`;
- registrar `UserCreated` na outbox após criação bem-sucedida;
- publicar eventos pendentes da outbox no RabbitMQ por publisher assíncrono.

Fora de escopo:

- login;
- JWT;
- refresh token;
- bloqueio/desbloqueio;
- `course-service`;
- `notification-service`;
- consumo de eventos.

## Premissas técnicas

- O `auth-user-service` deve ser criado como serviço isolado, com seu próprio módulo/diretório e banco PostgreSQL.
- O serviço deve usar Java 25 e Spring Boot 4.0.x.
- O build deve usar Gradle 9.x e dependências gerenciadas pelo plugin/BOM do Spring Boot sempre que possível.
- O serviço deve se conectar ao banco `auth_user_db` definido no `docker-compose.yml`.
- O serviço deve publicar eventos no RabbitMQ local definido no `docker-compose.yml`.
- Controllers devem apenas receber requisições, validar entrada, chamar casos de uso e retornar respostas.
- Regras de negócio devem ficar nas camadas de aplicação/domínio.
- Cada tarefa deve ser pequena o suficiente para revisão e commit isolado.
- O ADR-006 substitui a publicação direta do evento dentro da transação por registro transacional em outbox e publicação assíncrona posterior.

## Tasks

### T01 - Bootstrap auth-user-service Spring Boot

- **ID:** T01
- **Título:** Bootstrap auth-user-service Spring Boot
- **Objetivo:** Criar a estrutura inicial do `auth-user-service` como aplicação Spring Boot isolada, sem implementar regra de negócio.
- **Arquivos esperados:**
  - `settings.gradle`
  - `auth-user-service/build.gradle`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/AuthUserServiceApplication.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/AuthUserServiceApplicationTests.java`
- **Critérios de aceite:**
  - O módulo `auth-user-service` existe.
  - A aplicação Spring Boot inicia em teste de contexto.
  - O módulo usa Java 25.
  - O módulo usa Spring Boot 4.0.x.
  - Nenhum código de negócio foi implementado nesta tarefa.
- **Testes esperados:**
  - Teste de carregamento de contexto da aplicação.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `build: bootstrap auth user service`

### T02 - Configure application properties and database connection

- **ID:** T02
- **Título:** Configure application properties and database connection
- **Objetivo:** Configurar propriedades da aplicação, porta HTTP e conexão com o PostgreSQL próprio do `auth-user-service`.
- **Arquivos esperados:**
  - `auth-user-service/src/main/resources/application.yml`
  - `auth-user-service/build.gradle`
  - `auth-user-service/src/test/resources/application-test.yml`
- **Critérios de aceite:**
  - A aplicação aponta para o banco `auth_user_db`.
  - Usuário e senha de banco são compatíveis com a infraestrutura local.
  - A configuração de teste não depende do banco local diretamente quando testes automatizados usarem Testcontainers ou perfil dedicado.
  - O serviço não referencia bancos de outros microsserviços.
  - Dependências compatíveis com Spring Boot 4.0.x são gerenciadas pelo plugin/BOM do Spring Boot sempre que possível.
- **Testes esperados:**
  - Teste de contexto com profile de teste.
  - Validação de configuração de datasource.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `config: add auth user service database settings`

### T03 - Add Flyway migration for users table

- **ID:** T03
- **Título:** Add Flyway migration for users table
- **Objetivo:** Criar a primeira migração do banco do `auth-user-service` para persistir usuários e papéis.
- **Arquivos esperados:**
  - `auth-user-service/build.gradle`
  - `auth-user-service/src/main/resources/db/migration/V1__create_users_tables.sql`
- **Critérios de aceite:**
  - Existe tabela para usuários.
  - Existe estrutura para papéis do usuário.
  - E-mail possui restrição única.
  - Senha é armazenada em coluna de hash, não em coluna de texto puro.
  - Status permite `ACTIVE` e `BLOCKED`.
  - A migração pertence somente ao banco do `auth-user-service`.
- **Testes esperados:**
  - Teste de migração com banco de teste.
  - Teste de restrição única para e-mail, se já houver infraestrutura de persistência suficiente.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `db: add auth user schema migration`

### T04 - Create User domain model

- **ID:** T04
- **Título:** Create User domain model
- **Objetivo:** Criar o modelo de domínio para usuário, papéis e status, concentrando regras que não pertencem ao controller.
- **Arquivos esperados:**
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/domain/model/User.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/domain/model/UserRole.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/domain/model/UserStatus.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/domain/model/UserTest.java`
- **Critérios de aceite:**
  - `User` possui `id`, `name`, `email`, `passwordHash`, `status`, `roles`, `createdAt` e `updatedAt`.
  - `UserRole` possui `STUDENT`, `TEACHER` e `ADMIN`.
  - `UserStatus` possui `ACTIVE` e `BLOCKED`.
  - Usuários novos são criados como `ACTIVE`.
  - Usuário sem papel é rejeitado.
  - O domínio não conhece detalhes de HTTP, JPA ou RabbitMQ.
- **Testes esperados:**
  - Deve criar usuário válido com status `ACTIVE`.
  - Deve rejeitar nome inválido.
  - Deve rejeitar e-mail inválido.
  - Deve rejeitar senha hash ausente.
  - Deve rejeitar lista de papéis vazia.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: add auth user domain model`

### T05 - Create UserRepository

- **ID:** T05
- **Título:** Create UserRepository
- **Objetivo:** Criar a camada de persistência para salvar usuários e verificar unicidade de e-mail.
- **Arquivos esperados:**
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/port/UserRepository.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/persistence/UserJpaEntity.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/persistence/UserJpaRepository.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/persistence/JpaUserRepository.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/persistence/JpaUserRepositoryTest.java`
- **Critérios de aceite:**
  - Existe uma porta de repositório na camada de aplicação.
  - A implementação JPA fica na infraestrutura.
  - É possível salvar usuário.
  - É possível verificar existência por e-mail.
  - A restrição única de e-mail é respeitada.
  - O repositório acessa apenas o banco do `auth-user-service`.
- **Testes esperados:**
  - Deve persistir usuário válido.
  - Deve persistir papéis.
  - Deve persistir status `ACTIVE`.
  - Deve detectar e-mail existente.
  - Deve falhar para e-mail duplicado.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: add auth user repository`

### T06 - Create CreateUser use case/service

- **ID:** T06
- **Título:** Create CreateUser use case/service
- **Objetivo:** Implementar o caso de uso de criação de usuário sem HTTP, JPA direto ou RabbitMQ direto.
- **Arquivos esperados:**
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/usecase/CreateUserUseCase.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/usecase/CreateUserCommand.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/usecase/CreateUserResult.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/exception/UserEmailAlreadyExistsException.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/application/usecase/CreateUserUseCaseTest.java`
- **Critérios de aceite:**
  - O caso de uso valida e-mail duplicado antes de salvar.
  - O caso de uso delega hash de senha para uma porta dedicada.
  - O caso de uso retorna dados públicos do usuário criado.
  - O caso de uso não retorna senha nem hash.
  - O caso de uso não depende de controller, JPA ou RabbitMQ diretamente.
- **Testes esperados:**
  - Deve criar usuário válido.
  - Deve rejeitar e-mail duplicado.
  - Deve chamar o componente de hash.
  - Deve salvar usuário com hash, não senha pura.
  - Deve retornar resultado sem senha e sem hash.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: add create user use case`

### T07 - Create POST /users endpoint

- **ID:** T07
- **Título:** Create POST /users endpoint
- **Objetivo:** Expor o contrato HTTP `POST /users` para cadastro de usuário.
- **Arquivos esperados:**
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/web/UserController.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/web/CreateUserRequest.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/web/UserResponse.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/web/ApiErrorResponse.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/web/GlobalExceptionHandler.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/web/UserControllerTest.java`
- **Critérios de aceite:**
  - `POST /users` retorna `201 Created` para request válido.
  - Entrada inválida retorna `400 Bad Request`.
  - E-mail duplicado retorna `409 Conflict`.
  - Response de sucesso não contém senha nem hash.
  - Controller não contém regra de negócio.
- **Testes esperados:**
  - Teste HTTP para criação com sucesso.
  - Teste HTTP para nome inválido.
  - Teste HTTP para e-mail inválido.
  - Teste HTTP para senha inválida.
  - Teste HTTP para papéis inválidos.
  - Teste HTTP para e-mail duplicado.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: add create user endpoint`

### T08 - Add password hashing with BCrypt

- **ID:** T08
- **Título:** Add password hashing with BCrypt
- **Objetivo:** Implementar a estratégia de hash de senha com BCrypt conforme ADR-002.
- **Arquivos esperados:**
  - `auth-user-service/build.gradle`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/port/PasswordHasher.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/security/BCryptPasswordHasher.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/security/BCryptPasswordHasherTest.java`
- **Critérios de aceite:**
  - Senhas são transformadas em hash BCrypt.
  - Hash gerado é diferente da senha original.
  - Hash pode ser verificado contra a senha original quando necessário.
  - O strength factor pode ser configurado.
  - Nenhuma senha pura é persistida ou retornada.
- **Testes esperados:**
  - Deve gerar hash BCrypt.
  - Deve validar senha correta contra o hash.
  - Deve rejeitar senha incorreta contra o hash.
  - Deve garantir que o hash não é igual à senha original.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: add bcrypt password hashing`

### T09 - Add UserCreated event model

- **ID:** T09
- **Título:** Add UserCreated event model
- **Objetivo:** Criar o modelo de evento `UserCreated` com payload definido no FDD.
- **Arquivos esperados:**
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/event/UserCreatedEvent.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/event/UserCreatedPayload.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/application/event/UserCreatedEventTest.java`
- **Critérios de aceite:**
  - Evento possui `eventId`, `eventType`, `occurredAt` e `payload`.
  - `eventType` é `UserCreated`.
  - Payload possui `userId`, `name` e `email`.
  - Evento não contém senha nem hash.
  - Evento representa fato ocorrido após criação do usuário.
- **Testes esperados:**
  - Deve criar evento com `eventType` correto.
  - Deve gerar `eventId`.
  - Deve preencher `occurredAt`.
  - Deve preencher payload esperado.
  - Deve não expor dados sensíveis.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: add user created event model`

### T10 - Configure RabbitMQ producer

- **ID:** T10
- **Título:** Configure RabbitMQ producer
- **Objetivo:** Configurar infraestrutura de publicação de eventos no RabbitMQ sem acoplar o caso de uso diretamente ao broker.
- **Arquivos esperados:**
  - `auth-user-service/build.gradle`
  - `auth-user-service/src/main/resources/application.yml`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/port/EventPublisher.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/messaging/RabbitMqConfig.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/messaging/RabbitMqEventPublisher.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/messaging/RabbitMqEventPublisherTest.java`
- **Critérios de aceite:**
  - Existe uma porta de publicação de eventos.
  - Implementação RabbitMQ fica na infraestrutura.
  - Configurações de host, porta, usuário e senha vêm de configuração externa.
  - A decisão operacional de exchange/routing key fica documentada no código de configuração ou no plano da tarefa.
  - O publisher não publica senha nem hash.
- **Testes esperados:**
  - Teste de configuração do publisher.
  - Teste de serialização do evento.
  - Teste de publicação usando mock, fake ou Testcontainers.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: configure rabbitmq event publisher`

### T11 - Publish UserCreated after user creation

- **ID:** T11
- **Título:** Publish UserCreated after user creation
- **Objetivo:** Integrar o caso de uso de criação de usuário com a publicação do evento `UserCreated`.
- **Arquivos esperados:**
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/usecase/CreateUserUseCase.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/event/UserCreatedEventFactory.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/application/usecase/CreateUserUseCaseTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/integration/CreateUserIntegrationTest.java`
- **Critérios de aceite:**
  - `UserCreated` é publicado após persistência bem-sucedida.
  - Evento não é publicado se validação falhar.
  - Evento não é publicado se e-mail já existir.
  - Evento contém dados do usuário criado.
  - Logs incluem `eventId` quando o evento for publicado.
- **Testes esperados:**
  - Deve publicar evento após criação válida.
  - Não deve publicar evento em erro de validação.
  - Não deve publicar evento em e-mail duplicado.
  - Deve publicar payload conforme FDD.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: publish user created event`

### T12 - Add tests for user creation

- **ID:** T12
- **Título:** Add tests for user creation
- **Objetivo:** Consolidar a cobertura automatizada da entrega, cobrindo domínio, persistência, HTTP e mensageria.
- **Arquivos esperados:**
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/domain/model/UserTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/application/usecase/CreateUserUseCaseTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/persistence/JpaUserRepositoryTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/web/UserControllerTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/integration/CreateUserIntegrationTest.java`
- **Critérios de aceite:**
  - Todos os critérios de aceite do FDD possuem cobertura.
  - Casos de erro esperados possuem teste.
  - E-mail único é validado na aplicação e no banco.
  - Hash de senha é verificado.
  - Publicação de evento é verificada.
  - Testes não dependem de bancos de outros serviços.
- **Testes esperados:**
  - Unitários de domínio e aplicação.
  - Persistência com PostgreSQL de teste ou Testcontainers.
  - Controller tests para contrato HTTP.
  - Integração de mensageria com mock, fake ou Testcontainers.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `test: cover auth user creation flow`

### T13 - Add README section for auth-user-service

- **ID:** T13
- **Título:** Add README section for auth-user-service
- **Objetivo:** Documentar como executar, validar e entender a primeira entrega do `auth-user-service`.
- **Arquivos esperados:**
  - `README.md`
  - `auth-user-service/README.md`, se a documentação específica por serviço for adotada.
- **Critérios de aceite:**
  - README informa que o `auth-user-service` é responsável por criação de usuários.
  - README informa dependências locais: PostgreSQL e RabbitMQ.
  - README mostra comando para subir infraestrutura local.
  - README mostra comando para rodar testes do serviço.
  - README não documenta login, JWT ou serviços fora do escopo como se estivessem prontos.
- **Testes esperados:**
  - Não há teste automatizado obrigatório para documentação.
  - Revisão manual dos comandos documentados.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `docs: document auth user service`

## Ajuste do plano após ADR-006

As tasks `T10`, `T11` e parte de `T12` foram planejadas antes da adoção formal do Transactional Outbox Pattern.

Com a aceitação do ADR-006, a direção definitiva deixa de ser publicar `UserCreated` diretamente no fluxo transacional de criação de usuário. O fluxo final passa a ser:

1. criar o usuário;
2. registrar `UserCreated` na `outbox_events` na mesma transação;
3. publicar eventos pendentes por publisher assíncrono;
4. atualizar status e tentativas de publicação na outbox.

As tasks `T16` a `T19` abaixo formalizam essa fase e devem ser consideradas a referência para concluir o tópico de outbox no `auth-user-service`.

### T16 - Add outbox event persistence

- **ID:** T16
- **Título:** Add outbox event persistence
- **Status:** Implementada.
- **Objetivo:** Adicionar a persistência local dos eventos de domínio do `auth-user-service` por meio da tabela `outbox_events` e do adapter responsável por gravar a intenção de publicação dentro da transação do banco.
- **Escopo:**
  - criar a migration da tabela `outbox_events`;
  - definir constraints e índices necessários para busca, rastreabilidade e deduplicação;
  - criar a estrutura de persistência da outbox no `auth-user-service`;
  - preparar a porta de aplicação para gravar eventos de domínio na transação local.
- **Fora de escopo:**
  - alterar o fluxo de criação de usuário para usar a outbox;
  - publicar eventos no RabbitMQ;
  - criar consumers, DLQ ou retry de consumidor;
  - criar operação administrativa de reprocessamento.
- **Arquivos esperados:**
  - `auth-user-service/src/main/resources/db/migration/V2__create_outbox_events_table.sql`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/port/DomainEventRecorder.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/outbox/OutboxEventJpaEntity.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/outbox/OutboxEventJpaRepository.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/outbox/JpaDomainEventRecorder.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/persistence/OutboxMigrationTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/outbox/JpaDomainEventRecorderTest.java`
- **Critérios de aceite:**
  - a tabela `outbox_events` existe no banco do `auth-user-service`;
  - a tabela possui `status` com valores `PENDING`, `PUBLISHED` e `FAILED`;
  - a tabela possui `attempts` não negativo;
  - a tabela possui unicidade de `event_id`;
  - a tabela possui índice para busca por `(status, next_attempt_at)`;
  - a tabela possui índice para rastreio por aggregate;
  - existe uma porta de aplicação para registrar eventos na transação local;
  - a implementação de persistência da outbox fica isolada na infraestrutura.
- **Testes esperados:**
  - teste de migration garantindo criação da tabela `outbox_events`;
  - teste de constraints para `status`, `attempts` e `event_id`;
  - teste de persistência garantindo gravação de evento com payload JSONB sanitizado;
  - teste de persistência garantindo valores iniciais coerentes para status e tentativas.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: add outbox events persistence`

Resultado implementado:

- A tabela `outbox_events` foi criada com constraints, índices e unicidade de `event_id`.
- A gravação de eventos usa `JpaDomainEventRecorder`, `OutboxEventJpaEntity` e `OutboxEventJpaRepository`.
- O payload JSONB armazena o envelope sanitizado de `UserCreated`.

### T17 - Record UserCreated event in outbox

- **ID:** T17
- **Título:** Record UserCreated event in outbox
- **Status:** Implementada.
- **Objetivo:** Alterar o fluxo de criação de usuário para registrar `UserCreated` na outbox, em vez de publicar o evento diretamente no RabbitMQ dentro da transação do caso de uso.
- **Escopo:**
  - integrar `CreateUserUseCase` com `DomainEventRecorder`;
  - registrar `UserCreated` na mesma transação que persiste o usuário;
  - preservar o contrato HTTP e o comportamento funcional de criação de usuário;
  - remover o acoplamento direto entre caso de uso transacional e publicação no broker.
- **Fora de escopo:**
  - criar o publisher assíncrono da outbox;
  - implementar política de retry do publisher;
  - alterar exchange, routing key ou topologia do RabbitMQ;
  - implementar consumo de `UserCreated`.
- **Arquivos esperados:**
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/usecase/CreateUserUseCase.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/application/event/UserCreatedEventFactory.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/application/usecase/CreateUserUseCaseTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/integration/CreateUserTransactionTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/integration/CreateUserIntegrationTest.java`
- **Critérios de aceite:**
  - `CreateUserUseCase` registra `UserCreated` na outbox após persistir usuário válido;
  - o caso de uso não publica diretamente no RabbitMQ;
  - usuário e evento de outbox são confirmados ou revertidos juntos na transação do banco;
  - falha de validação não registra evento na outbox;
  - e-mail duplicado não registra evento na outbox;
  - o endpoint `POST /users` mantém semântica pública inalterada.
- **Testes esperados:**
  - teste de aplicação garantindo uso de `DomainEventRecorder`;
  - teste garantindo que `CreateUserUseCase` não chama publisher RabbitMQ diretamente;
  - teste transacional garantindo rollback conjunto de usuário e evento de outbox;
  - teste de integração garantindo registro de `UserCreated` na outbox após criação bem-sucedida.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: record user created events in outbox`

Resultado implementado:

- `CreateUserUseCase` registra `UserCreated` por meio de `DomainEventRecorder`.
- O caso de uso não depende diretamente do publisher RabbitMQ.
- Testes cobrem sucesso, falhas de validação, e-mail duplicado e rollback transacional.

### T18 - Add outbox publisher to RabbitMQ

- **ID:** T18
- **Título:** Add outbox publisher to RabbitMQ
- **Status:** Implementada.
- **Objetivo:** Criar o publisher assíncrono que busca eventos pendentes na `outbox_events`, publica no RabbitMQ e atualiza o estado de publicação no banco do `auth-user-service`.
- **Escopo:**
  - buscar apenas eventos `PENDING` com `next_attempt_at` vencido;
  - publicar o payload sanitizado do evento no RabbitMQ;
  - marcar eventos publicados como `PUBLISHED`;
  - registrar falhas, incrementar tentativas e reagendar `next_attempt_at`;
  - marcar eventos como `FAILED` quando o limite de tentativas for atingido.
- **Fora de escopo:**
  - reprocessamento manual de eventos `FAILED`;
  - consumer retry e DLQ do `notification-service`;
  - limpeza de registros antigos da outbox;
  - novos tipos de evento além de `UserCreated`.
- **Arquivos esperados:**
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/outbox/OutboxEventPublisher.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/messaging/RabbitMqEventPublisher.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/outbox/OutboxEventJpaRepository.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/outbox/OutboxEventJpaEntity.java`
  - `auth-user-service/src/main/java/com/yuriromao/ead/authuser/infrastructure/outbox/OutboxEventStatus.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/outbox/OutboxEventPublisherTest.java`
- **Critérios de aceite:**
  - existe um publisher assíncrono separado do caso de uso transacional;
  - apenas eventos `PENDING` elegíveis são buscados para publicação;
  - publicação bem-sucedida atualiza o evento para `PUBLISHED`;
  - falha de publicação incrementa tentativas e registra erro;
  - falha final marca o evento como `FAILED`;
  - logs de publicação incluem `eventId`, `eventType`, status e tentativa;
  - `RabbitMqEventPublisher` é usado pelo publisher da outbox, não diretamente pelo caso de uso.
- **Testes esperados:**
  - teste garantindo publicação de eventos pendentes;
  - teste garantindo marcação como `PUBLISHED` após sucesso;
  - teste garantindo retry com atualização de `attempts`, `last_error` e `next_attempt_at`;
  - teste garantindo transição para `FAILED` ao atingir o limite de tentativas;
  - teste garantindo que payload publicado preserva o envelope sanitizado.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
- **Mensagem de commit sugerida:**
  - `feat: publish pending outbox events`

Resultado implementado:

- `OutboxEventPublisher` publica eventos `PENDING` elegíveis em lotes configuráveis.
- Sucesso atualiza status para `PUBLISHED`, preenche `publishedAt` e incrementa `attempts`.
- Falha registra `lastError`, incrementa `attempts`, reagenda `nextAttemptAt` e marca `FAILED` ao esgotar `max-attempts`.
- O publisher usa `RabbitMqEventPublisher` por meio da porta de publicação de eventos.

### T19 - Cover outbox event publishing

- **ID:** T19
- **Título:** Cover outbox event publishing
- **Status:** Consolidada.
- **Objetivo:** Consolidar a cobertura automatizada do fluxo completo de outbox do `auth-user-service`, cobrindo persistência, transação, publicação assíncrona, retry e falha final.
- **Escopo:**
  - complementar testes de migration, persistência e integração;
  - validar o fluxo de criação de usuário com registro de outbox;
  - validar publicação assíncrona, retry e status finais;
  - proteger o contrato de que eventos não expõem senha nem hash.
- **Fora de escopo:**
  - testes de consumo do `notification-service`;
  - testes de DLQ do consumidor;
  - operação de reprocessamento manual;
  - cobertura de eventos futuros além de `UserCreated`.
- **Arquivos esperados:**
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/integration/CreateUserIntegrationTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/integration/CreateUserTransactionTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/outbox/OutboxEventPublisherTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/outbox/JpaDomainEventRecorderTest.java`
  - `auth-user-service/src/test/java/com/yuriromao/ead/authuser/infrastructure/persistence/OutboxMigrationTest.java`
- **Critérios de aceite:**
  - o fluxo de outbox possui cobertura automatizada suficiente para critérios do ADR-006;
  - criação de usuário registra `UserCreated` na outbox;
  - rollback transacional impede persistência parcial de usuário ou evento;
  - relay publica eventos elegíveis e atualiza estado corretamente;
  - falhas de publicação são observáveis e atualizam tentativas;
  - payload publicado não contém senha nem hash;
  - build do módulo passa com a cobertura exigida.
- **Testes esperados:**
  - testes unitários e de integração para registro de outbox;
  - testes de persistência para schema e constraints de `outbox_events`;
  - testes do publisher para sucesso, retry e falha final;
  - testes garantindo ausência de dados sensíveis no evento publicado;
  - testes cobrindo segurança contra duplicidade por `eventId` no limite do producer.
- **Comando de validação:**
  - `./gradlew :auth-user-service:test`
  - `./gradlew :auth-user-service:build`
- **Mensagem de commit sugerida:**
  - `test: cover outbox event publishing`

Resultado consolidado:

- A cobertura automatizada protege o registro de `UserCreated` na outbox, rollback transacional, payload sem dados sensíveis, publicação assíncrona, retry, falha final e unicidade de `event_id`.
- O build do módulo executa testes, Spotless e verificação JaCoCo.

Riscos remanescentes da outbox:

- Reprocessamento manual de eventos `FAILED` ainda não foi implementado.
- Limpeza ou arquivamento de registros antigos da outbox ainda não foi definido.
- Idempotência dos consumers futuros ainda precisa ser implementada por serviço consumidor.
- Métricas e dashboards específicos para eventos pendentes, publicados e com falha ainda precisam ser definidos.

## Validação final da entrega

Ao concluir todas as tasks, executar:

```bash
docker compose up -d
./gradlew :auth-user-service:test
./gradlew :auth-user-service:build
```

Critérios finais:

- O serviço compila.
- Todos os testes passam.
- `POST /users` cumpre o contrato do FDD.
- Senha nunca é persistida, retornada, logada ou publicada em evento em texto puro.
- `UserCreated` é registrado na outbox após criação bem-sucedida.
- Eventos pendentes da outbox podem ser publicados por publisher assíncrono com controle de status e tentativas.
- O serviço acessa somente o banco do `auth-user-service`.

## Commit sugerido para este plano

```bash
git add docs/implementation-plans/plan-001-auth-user-service.md
git commit -m "docs: add auth user service implementation plan"
```
