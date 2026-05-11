# ADR-006: Transactional Outbox for Domain Events

## Status

Accepted

## Context

O `auth-user-service` publica eventos de domínio para integrar o bounded context Auth/User com outros serviços da EAD Platform. O primeiro evento desse fluxo é `UserCreated`, publicado após a criação bem-sucedida de um usuário.

No estado atual, o `CreateUserUseCase` salva o usuário no banco `auth_user_db` e publica `UserCreated` diretamente no RabbitMQ dentro de um método `@Transactional`. Isso cria um risco de inconsistência porque o PostgreSQL e o RabbitMQ não compartilham a mesma transação. A transação do banco controla apenas as alterações locais no banco do `auth-user-service`; ela não confirma nem desfaz a publicação no broker.

Esse desenho permite cenários problemáticos:

- o usuário é salvo no banco, mas a publicação no RabbitMQ falha; nesse caso, o fato de domínio existe, mas nenhum consumidor recebe `UserCreated`;
- o evento é publicado no RabbitMQ, mas a transação do banco falha ou sofre rollback depois da tentativa de publicação; nesse caso, consumidores podem observar um evento para um usuário que não foi persistido;
- a aplicação pode cair entre a persistência local e a publicação do evento, deixando o sistema sem registro confiável de que o evento ainda precisa ser publicado;
- uma retentativa manual ou automática pode publicar eventos duplicados se não houver controle por `eventId` e estado de publicação.

Como a plataforma segue microservices com database per service, a fonte de verdade do fato local deve continuar sendo o banco do serviço produtor. O RabbitMQ deve transportar eventos entre serviços, mas não deve ser tratado como parte da transação local do banco.

## Decision

Usaremos o `Transactional Outbox Pattern` para eventos de domínio publicados pelo `auth-user-service`.

Quando um caso de uso produzir um evento de domínio, o evento deve ser salvo em uma tabela `outbox_events` no mesmo banco e na mesma transação que persiste a alteração de estado local. No fluxo de criação de usuário, o `CreateUserUseCase` deve salvar o usuário e registrar o evento `UserCreated` na `outbox_events` dentro da mesma transação do PostgreSQL.

Um publisher assíncrono separado deve ler eventos pendentes da `outbox_events`, publicar esses eventos no RabbitMQ e atualizar o estado de publicação no banco. Esse publisher deve ser projetado para suportar retentativas, rastreabilidade por `eventId` e processamento seguro em caso de falhas temporárias do broker.

## Consequences

### Positive

- Garante que a criação do usuário e o registro do evento a publicar sejam confirmados ou desfeitos juntos na transação do banco.
- Reduz o risco de perder `UserCreated` após persistir um usuário.
- Permite retentativa de publicação sem depender de reconstituir eventos a partir de logs ou processos manuais.
- Mantém o `RabbitMQ` fora da transação local do PostgreSQL, preservando simplicidade operacional e evitando transações distribuídas.
- Cria uma trilha auditável de eventos pendentes, publicados e com falha.
- Facilita observabilidade de publicação por `eventId`, `eventType`, status e tentativas.
- Prepara a base para consumidores idempotentes, pois eventos podem ser republicados em cenários de retry.

### Negative

- Aumenta a complexidade do `auth-user-service` com tabela, persistência e publisher assíncrono.
- Introduz consistência eventual: o usuário pode ser criado antes do evento ser efetivamente entregue ao RabbitMQ.
- Exige política de retry, backoff, concorrência e limpeza de registros antigos.
- Exige atenção para evitar publicação duplicada em cenários de falha após envio ao RabbitMQ e antes de marcar o evento como publicado.
- Exige testes adicionais de persistência, agendamento/publicação assíncrona e falhas de broker.

## Alternatives Considered

### Publicação direta no RabbitMQ

Publicar diretamente no RabbitMQ dentro do `CreateUserUseCase` é simples e já permite integração inicial com consumidores. A alternativa foi rejeitada como estratégia final porque o banco e o broker não participam da mesma transação. Falhas entre `save user` e `publish UserCreated` podem gerar perda de evento ou observação de evento incompatível com o estado persistido.

### `@TransactionalEventListener(AFTER_COMMIT)`

Usar `@TransactionalEventListener(AFTER_COMMIT)` evita publicar o evento antes do commit da transação do banco. Essa alternativa reduz o risco de publicar um evento para uma transação que sofreu rollback. Porém, ela não resolve a falha após o commit e antes ou durante a publicação no RabbitMQ. Se a aplicação cair ou o broker estiver indisponível após o commit, o evento pode ser perdido porque não existe registro persistido de publicação pendente.

### Outbox Pattern

O `Outbox Pattern` foi aceito porque registra o evento no mesmo banco e na mesma transação do fato local. A publicação deixa de depender do sucesso imediato do broker durante o caso de uso. Eventos pendentes podem ser publicados posteriormente por um processo assíncrono, com retry, observabilidade e controle de status.

## Implementation Direction

A implementação deve ser feita em tarefas futuras, sem alterar a semântica pública do endpoint `POST /users`.

Direção técnica esperada:

- criar a tabela `outbox_events` no banco do `auth-user-service`;
- armazenar ao menos `eventId`, `eventType`, `occurredAt`, payload serializado, status de publicação, número de tentativas, timestamps de criação/atualização e informações de erro quando houver falha;
- criar a porta `DomainEventRecorder` na camada de aplicação;
- criar um adapter de persistência para gravar eventos na `outbox_events`;
- alterar `CreateUserUseCase` para gravar `UserCreated` na outbox, em vez de publicar diretamente no RabbitMQ;
- criar um publisher assíncrono para buscar eventos pendentes e publicá-los no RabbitMQ;
- atualizar `RabbitMqEventPublisher` para ser usado pelo publisher assíncrono, não diretamente pelo caso de uso transacional;
- definir política inicial de status, retry e backoff;
- garantir que o publisher registre logs com `eventId`, `eventType`, status e tentativa;
- atualizar testes unitários, de persistência, integração e mensageria.

Esta decisão não define ainda a topologia definitiva de exchanges, queues, routing keys, retry queues ou dead-letter queues. Esses detalhes devem ser registrados em ADR própria quando forem estabilizados.

## Validation

Testes esperados para a implementação futura:

- teste de migration garantindo a criação da tabela `outbox_events`;
- teste de persistência garantindo que `UserCreated` é salvo na outbox junto com o usuário;
- teste transacional garantindo rollback conjunto de usuário e evento de outbox;
- teste de aplicação garantindo que `CreateUserUseCase` usa `DomainEventRecorder`;
- teste garantindo que o caso de uso não chama RabbitMQ diretamente;
- teste do publisher assíncrono garantindo publicação de eventos pendentes no RabbitMQ;
- teste garantindo atualização de status após publicação bem-sucedida;
- teste garantindo incremento de tentativas e registro de erro quando a publicação falhar;
- teste garantindo que payload publicado não contém senha nem hash;
- teste de idempotência ou segurança contra publicação duplicada por `eventId`, quando a estratégia for definida.

Comandos Gradle esperados:

```bash
./gradlew :auth-user-service:test
./gradlew :auth-user-service:build
```
