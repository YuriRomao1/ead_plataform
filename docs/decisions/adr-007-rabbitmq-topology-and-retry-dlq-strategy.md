# ADR-007: RabbitMQ Topology and Retry/DLQ Strategy

## Status

Accepted

## Context

A EAD Platform usa RabbitMQ para transportar eventos de domínio entre microsserviços. O `auth-user-service` já produz `UserCreated` por meio da outbox transacional definida no ADR-006. Serviços futuros, como `course-service`, também produzirão eventos, e o `notification-service` será o primeiro consumidor de `UserCreated` e `EnrollmentCreated`.

Até agora, o projeto possuía uma convenção operacional mínima no `auth-user-service`: publicar eventos em um exchange configurável (`ead.domain.events`) usando routing keys específicas, como `auth-user.user-created`. Essa convenção ainda não definia uma topologia formal de exchanges, filas, retry e dead-letter queues.

Sem uma decisão explícita, cada serviço poderia criar nomes e políticas diferentes para filas e retentativas. Isso dificultaria observabilidade, operação local, testes de integração e evolução dos consumidores.

Também é importante separar dois tipos de falha:

- falha do produtor ao publicar no RabbitMQ depois de persistir o fato local;
- falha do consumidor ao processar uma mensagem já entregue pelo RabbitMQ.

O primeiro caso é tratado pela outbox do serviço produtor. O segundo caso deve ser tratado pela topologia de filas, retry e DLQ do serviço consumidor.

## Decision

Usaremos uma topologia RabbitMQ baseada em topic exchanges duráveis, routing keys explícitas por evento e filas de consumidor pertencentes ao serviço consumidor.

### Exchanges

Exchange principal de eventos:

- nome: `ead.domain.events`;
- tipo: `topic`;
- durável: sim;
- auto-delete: não;
- responsabilidade: receber eventos de domínio publicados por producers.

Exchange de retry para consumidores:

- nome: `ead.domain.events.retry`;
- tipo: `topic`;
- durável: sim;
- auto-delete: não;
- responsabilidade: receber mensagens rejeitadas temporariamente por consumidores e redirecioná-las para filas de retry com TTL.

Exchange de dead-letter:

- nome: `ead.domain.events.dlx`;
- tipo: `topic`;
- durável: sim;
- auto-delete: não;
- responsabilidade: receber mensagens que não podem mais ser processadas automaticamente.

### Routing keys

Routing keys devem seguir o padrão:

```text
<producer-context>.<event-name-kebab-case>
```

Eventos iniciais:

| Evento | Producer | Routing key |
| --- | --- | --- |
| `UserCreated` | `auth-user-service` | `auth-user.user-created` |
| `EnrollmentCreated` | `course-service` | `course.enrollment-created` |

Routing keys auxiliares:

| Uso | Padrão |
| --- | --- |
| Retry | `<routing-key>.retry` |
| DLQ | `<routing-key>.dlq` |

Exemplos:

- `auth-user.user-created.retry`;
- `auth-user.user-created.dlq`;
- `course.enrollment-created.retry`;
- `course.enrollment-created.dlq`.

### Filas de consumidores

Cada consumidor deve ser dono das próprias filas. Producers não devem declarar filas de outros serviços.

Padrão de nomes:

```text
<consumer-service>.<event-name-kebab-case>.queue
<consumer-service>.<event-name-kebab-case>.retry.queue
<consumer-service>.<event-name-kebab-case>.dlq
```

Filas iniciais esperadas para `notification-service`:

| Evento | Fila principal | Fila de retry | DLQ |
| --- | --- | --- | --- |
| `UserCreated` | `notification-service.user-created.queue` | `notification-service.user-created.retry.queue` | `notification-service.user-created.dlq` |
| `EnrollmentCreated` | `notification-service.enrollment-created.queue` | `notification-service.enrollment-created.retry.queue` | `notification-service.enrollment-created.dlq` |

A fila principal deve ser vinculada ao exchange `ead.domain.events` com a routing key do evento.

A fila de retry deve ser vinculada ao exchange `ead.domain.events.retry` com a routing key `<routing-key>.retry`. Ela deve possuir TTL configurável e dead-letter de volta para `ead.domain.events` com a routing key original.

A DLQ deve ser vinculada ao exchange `ead.domain.events.dlx` com a routing key `<routing-key>.dlq`.

### Retry no produtor

Falhas de publicação do produtor no RabbitMQ devem continuar sendo tratadas pela outbox transacional do serviço produtor, não por filas RabbitMQ de retry.

Para o `auth-user-service`, a política inicial é:

- buscar apenas eventos `PENDING` com `next_attempt_at` vencido;
- publicar em lotes configuráveis;
- ao publicar com sucesso, marcar o evento como `PUBLISHED`;
- ao falhar, incrementar `attempts`, registrar `last_error` e reagendar `next_attempt_at`;
- ao atingir o máximo de tentativas, marcar como `FAILED`.

Valores iniciais configuráveis no `auth-user-service`:

| Propriedade | Valor inicial |
| --- | --- |
| `auth-user-service.outbox.publisher.fixed-delay` | `5000` |
| `auth-user-service.outbox.publisher.batch-size` | `20` |
| `auth-user-service.outbox.publisher.max-attempts` | `5` |
| `auth-user-service.outbox.publisher.retry-delay` | `PT30S` |

Eventos `FAILED` permanecem no banco do produtor para análise e reprocessamento operacional futuro. A forma de reprocessamento administrativo ainda deve ser definida em tarefa própria.

### Retry no consumidor

Falhas transitórias de consumo devem ser tratadas com retry via RabbitMQ:

1. consumidor recebe a mensagem da fila principal;
2. se o processamento falhar de forma transitória, a mensagem é rejeitada sem requeue;
3. a fila principal encaminha a mensagem para `ead.domain.events.retry` com routing key `<routing-key>.retry`;
4. a fila de retry segura a mensagem pelo TTL configurado;
5. após o TTL, a mensagem volta para `ead.domain.events` com a routing key original;
6. o consumidor tenta processar novamente.

O número máximo inicial de tentativas de consumo deve ser `5`. Quando o limite for alcançado, o consumidor deve encaminhar a mensagem para `ead.domain.events.dlx` com routing key `<routing-key>.dlq`.

Mensagens inválidas, eventos desconhecidos, payloads incompatíveis ou mensagens que violem regras não transitórias devem ir para DLQ sem retry automático.

### Idempotência

Consumidores devem ser idempotentes por `eventId`.

Antes de executar efeitos colaterais, o consumidor deve verificar se o `eventId` já foi processado no próprio banco do serviço consumidor. Se já foi processado, a mensagem deve ser confirmada sem repetir o efeito colateral.

Essa decisão define a obrigação arquitetural de idempotência, mas a modelagem física da tabela de eventos processados do `notification-service` deve ser detalhada na implementação desse serviço.

### Observabilidade

Logs de publicação e consumo devem incluir:

- `eventId`;
- `eventType`;
- routing key;
- exchange;
- queue, quando aplicável;
- tentativa atual;
- status final (`PUBLISHED`, retry agendado, DLQ, processado).

Métricas futuras devem distinguir:

- eventos pendentes na outbox;
- eventos `FAILED` na outbox;
- mensagens nas filas principais;
- mensagens nas filas de retry;
- mensagens em DLQ.

## Consequences

### Positive

- Padroniza nomes de exchanges, routing keys e filas.
- Mantém producers desacoplados das filas dos consumidores.
- Separa claramente retry de publicação no produtor e retry de processamento no consumidor.
- Facilita monitoramento local pelo RabbitMQ Management UI.
- Prepara o `notification-service` para consumo idempotente e tratamento de falhas.
- Reduz decisões implícitas em código e configurações.

### Negative

- Aumenta a quantidade de recursos RabbitMQ por evento consumido.
- Exige que consumidores implementem classificação de falhas transitórias e não transitórias.
- Exige cuidado para não criar loops infinitos entre fila principal e fila de retry.
- Exige armazenamento local de `eventId` processado para idempotência.
- Reprocessamento de mensagens em DLQ e eventos `FAILED` na outbox ainda precisa de operação explícita.

## Alternatives Considered

### Uma única fila compartilhada por todos os consumidores

Rejeitada porque acopla consumidores, dificulta isolamento de falhas e impede que serviços evoluam suas próprias políticas de retry.

### Producers declararem todas as filas de consumidores

Rejeitada porque aumenta acoplamento entre serviços. O producer deve conhecer apenas exchange e routing key do evento que publica.

### Retry apenas no código do consumidor

Rejeitada como estratégia principal porque retries em memória se perdem quando o processo reinicia e podem bloquear threads de consumo. O retry com fila e TTL deixa a falha observável no broker.

### DLQ única para todos os eventos

Rejeitada porque mistura falhas de eventos e consumidores diferentes. DLQs por consumidor e evento facilitam diagnóstico e reprocessamento seletivo.

## Implementation Direction

Próximas tarefas devem:

- atualizar comentários/configurações do `auth-user-service` para referenciar esta ADR;
- manter `auth-user-service` publicando em `ead.domain.events` com routing key `auth-user.user-created`;
- criar, no `notification-service`, filas principais, retry queues e DLQs para `UserCreated` e `EnrollmentCreated`;
- implementar idempotência por `eventId` no `notification-service`;
- implementar testes de configuração RabbitMQ para bindings, TTL, dead-letter exchanges e routing keys;
- implementar testes de consumo cobrindo sucesso, duplicidade, retry e DLQ;
- documentar processo operacional mínimo para eventos `FAILED` na outbox e mensagens em DLQ.

## Validation

Validações esperadas em implementações futuras:

- teste de configuração garantindo que `ead.domain.events` é topic exchange durável;
- teste de configuração garantindo que filas principais do consumidor estão ligadas às routing keys esperadas;
- teste de configuração garantindo que retry queues possuem TTL e dead-letter de volta para o exchange principal;
- teste de configuração garantindo que DLQs estão ligadas ao exchange `ead.domain.events.dlx`;
- teste de producer garantindo publicação de `UserCreated` em `auth-user.user-created`;
- teste de consumer garantindo processamento idempotente por `eventId`;
- teste de consumer garantindo retry em falha transitória;
- teste de consumer garantindo DLQ em payload inválido ou limite de tentativas excedido.
