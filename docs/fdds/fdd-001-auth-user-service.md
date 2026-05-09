# FDD-001: Auth/User Service

## 1. Contexto técnico

O `auth-user-service` é o microsserviço responsável pelo bounded context Auth/User da EAD Platform. Ele será a fonte de verdade para usuários, credenciais, papéis e status de conta.

Este FDD segue:

- `docs/domain-context.md`;
- `docs/hld.md`;
- `docs/decisions/adr-001-microservices-database-per-service.md`.

O serviço deve possuir seu próprio banco PostgreSQL, conforme a decisão de database per service. Nenhum outro microsserviço deve acessar diretamente esse banco.

A primeira versão do serviço cobre o cadastro de usuários e a publicação do evento `UserCreated` após uma criação bem-sucedida.

## 2. Objetivos técnicos

- Implementar o primeiro fluxo funcional do `auth-user-service`: criação de usuário.
- Persistir usuários no banco próprio do serviço.
- Validar `name`, `email`, `password` e `roles`.
- Garantir unicidade de e-mail dentro do `auth-user-service`.
- Armazenar senha apenas como hash.
- Suportar os papéis `STUDENT`, `TEACHER` e `ADMIN`.
- Suportar os status `ACTIVE` e `BLOCKED`.
- Criar usuários inicialmente com status `ACTIVE`.
- Publicar o evento `UserCreated` após a persistência do usuário.
- Expor contrato HTTP inicial para cadastro de usuário.

## 3. Escopo

Incluído nesta versão:

- criação de usuário;
- validação obrigatória de nome, e-mail e senha;
- validação de formato de e-mail;
- validação mínima de senha;
- hash de senha antes da persistência;
- validação de papéis permitidos;
- garantia de ao menos um papel por usuário;
- persistência de usuário, papéis e status;
- status inicial `ACTIVE`;
- suporte ao status `BLOCKED` no modelo;
- publicação do evento `UserCreated`;
- tratamento padronizado de erros para o endpoint de criação;
- logs mínimos para criação de usuário e publicação de evento;
- testes unitários, de persistência, mensageria e contrato HTTP.

## 4. Fora de escopo

Não faz parte desta versão:

- login;
- refresh token;
- geração de token;
- validação de token;
- logout;
- bloqueio de usuário;
- desbloqueio de usuário;
- troca ou recuperação de senha;
- atualização de usuário;
- remoção de usuário;
- consulta pública ou interna de usuários;
- implementação do `course-service`;
- implementação do `notification-service`;
- consumo de eventos;
- API Gateway;
- autenticação entre microsserviços.

## 5. Entidades

### User

Representa um usuário cadastrado na plataforma.

Campos esperados:

- `id`: identificador único.
- `name`: nome do usuário.
- `email`: e-mail único dentro do `auth-user-service`.
- `passwordHash`: senha armazenada exclusivamente como hash.
- `status`: status da conta.
- `roles`: conjunto de papéis atribuídos ao usuário.
- `createdAt`: data e hora de criação.
- `updatedAt`: data e hora da última atualização.

Regras relacionadas:

- `name` é obrigatório e não pode ser vazio.
- `email` é obrigatório, deve ter formato válido e deve ser único.
- `passwordHash` deve ser gerado a partir da senha recebida.
- `roles` deve conter ao menos um papel válido.
- `status` deve iniciar como `ACTIVE`.

### Role

Papéis permitidos:

- `STUDENT`;
- `TEACHER`;
- `ADMIN`.

### UserStatus

Status permitidos:

- `ACTIVE`;
- `BLOCKED`.

Nesta versão, usuários são criados como `ACTIVE`. O fluxo de bloqueio fica fora do escopo.

## 6. Endpoints

### POST `/api/v1/users`

Cria um novo usuário.

Request body:

```json
{
  "name": "User Name",
  "email": "user@email.com",
  "password": "plainPassword",
  "roles": ["STUDENT"]
}
```

Regras do request:

- `name` é obrigatório.
- `email` é obrigatório e deve possuir formato válido.
- `password` é obrigatória.
- `roles` é obrigatório e deve possuir ao menos um item.
- Cada item de `roles` deve ser um dos valores permitidos: `STUDENT`, `TEACHER` ou `ADMIN`.

Response de sucesso:

Status HTTP: `201 Created`

```json
{
  "id": "uuid",
  "name": "User Name",
  "email": "user@email.com",
  "status": "ACTIVE",
  "roles": ["STUDENT"],
  "createdAt": "2026-01-01T10:00:00Z"
}
```

Regras da resposta:

- A resposta não deve retornar senha nem hash de senha.
- O usuário retornado deve refletir o estado persistido.
- A criação bem-sucedida deve publicar `UserCreated`.

## 7. Eventos publicados

### UserCreated

Publicado pelo `auth-user-service` quando um novo usuário é criado com sucesso.

Consumidor esperado:

- `notification-service`.

Payload inicial:

```json
{
  "eventId": "uuid",
  "eventType": "UserCreated",
  "occurredAt": "2026-01-01T10:00:00Z",
  "payload": {
    "userId": "uuid",
    "name": "User Name",
    "email": "user@email.com"
  }
}
```

Regras do evento:

- O evento representa um fato já ocorrido.
- O evento deve ser publicado somente após a criação do usuário.
- `eventId` deve ser único.
- `occurredAt` deve representar o momento da ocorrência do evento.
- O payload não deve conter senha nem hash de senha.

A convenção definitiva de exchange, fila e routing key ainda não está definida no HLD e deve ser tratada em documentação própria ou no plano de implementação quando necessário.

## 8. Erros

Erros esperados para `POST /api/v1/users`:

| Situação | Status HTTP | Código sugerido |
| --- | ---: | --- |
| Nome ausente ou vazio | 400 | `USER_NAME_REQUIRED` |
| E-mail ausente | 400 | `USER_EMAIL_REQUIRED` |
| E-mail inválido | 400 | `USER_EMAIL_INVALID` |
| Senha ausente | 400 | `USER_PASSWORD_REQUIRED` |
| Senha fora da política mínima | 400 | `USER_PASSWORD_INVALID` |
| Lista de papéis ausente ou vazia | 400 | `USER_ROLE_REQUIRED` |
| Papel inválido | 400 | `USER_ROLE_INVALID` |
| E-mail já cadastrado | 409 | `USER_EMAIL_ALREADY_EXISTS` |
| Falha inesperada | 500 | `INTERNAL_ERROR` |

Formato esperado de erro:

```json
{
  "code": "USER_EMAIL_ALREADY_EXISTS",
  "message": "Email already exists.",
  "details": []
}
```

Regras de erro:

- Mensagens externas não devem expor detalhes internos de persistência ou mensageria.
- Erros de validação devem ser consistentes e testáveis.
- Erros inesperados devem ser registrados em log com contexto técnico suficiente para diagnóstico.

## 9. Segurança

- Senhas nunca devem ser persistidas em texto puro.
- Senhas nunca devem ser retornadas em respostas HTTP.
- Hash de senha nunca deve ser retornado em respostas HTTP.
- Logs não devem conter senha nem hash de senha.
- O algoritmo de hash deve ser adequado para senhas, como BCrypt.
- O endpoint de criação de usuário pode ser público nesta primeira versão, pois login e autorização ainda estão fora do escopo.
- Papéis enviados no cadastro devem ser validados contra a lista permitida.
- O status inicial deve ser `ACTIVE`.
- Usuários `BLOCKED` serão relevantes para autenticação futura, mas autenticação está fora do escopo desta versão.

## 10. Observabilidade

O serviço deve registrar logs mínimos para:

- recebimento de solicitação de criação de usuário, sem dados sensíveis;
- sucesso na criação de usuário;
- falha de validação;
- conflito de e-mail duplicado;
- publicação bem-sucedida do evento `UserCreated`;
- falha na publicação do evento `UserCreated`.

Métricas e health checks esperados:

- health check da aplicação;
- health check de conexão com PostgreSQL;
- health check de conexão com RabbitMQ, quando a integração de mensageria estiver ativa;
- métrica ou log estruturado para tentativas de criação de usuário;
- métrica ou log estruturado para falhas de criação de usuário;
- métrica ou log estruturado para publicação de eventos.

Quando houver suporte a correlação, logs de requisição e evento devem carregar um identificador de correlação ou o `eventId`.

## 11. Testes esperados

### Testes unitários de domínio/aplicação

- Deve criar usuário válido com status `ACTIVE`.
- Deve rejeitar nome ausente ou vazio.
- Deve rejeitar e-mail ausente.
- Deve rejeitar e-mail inválido.
- Deve rejeitar senha ausente.
- Deve rejeitar senha fora da política mínima.
- Deve rejeitar lista de papéis vazia.
- Deve rejeitar papel inválido.
- Deve garantir que senha seja convertida em hash antes da persistência.
- Deve garantir que a senha original não seja exposta no objeto de resposta.

### Testes de persistência

- Deve persistir usuário com dados válidos.
- Deve persistir papéis associados ao usuário.
- Deve impedir e-mail duplicado.
- Deve recuperar usuário pelo e-mail para validação de unicidade.

### Testes de mensageria

- Deve publicar `UserCreated` após criação bem-sucedida.
- Não deve publicar `UserCreated` quando a criação falhar.
- O evento publicado deve conter `eventId`, `eventType`, `occurredAt` e payload esperado.
- O evento publicado não deve conter senha nem hash de senha.

### Testes de controller/contrato HTTP

- `POST /api/v1/users` deve retornar `201 Created` para request válido.
- `POST /api/v1/users` deve retornar `400 Bad Request` para dados inválidos.
- `POST /api/v1/users` deve retornar `409 Conflict` para e-mail duplicado.
- A resposta de sucesso não deve conter senha nem hash de senha.
- A resposta de erro deve seguir o formato padronizado.

## 12. Critérios de aceite

- O endpoint `POST /api/v1/users` cria usuários válidos.
- Usuários são persistidos no banco próprio do `auth-user-service`.
- E-mails duplicados são rejeitados.
- Senhas são persistidas apenas como hash.
- A resposta de criação não expõe senha nem hash.
- Usuários são criados com status `ACTIVE`.
- Os papéis `STUDENT`, `TEACHER` e `ADMIN` são aceitos.
- Papéis inválidos são rejeitados.
- Usuários sem papel são rejeitados.
- O evento `UserCreated` é publicado após criação bem-sucedida.
- O evento `UserCreated` não expõe dados sensíveis.
- Os testes esperados são implementados e passam.
- O projeto compila.
- Nenhum outro serviço ou banco de outro serviço é acessado diretamente.

## 13. Riscos

- Publicar o evento após persistir o usuário sem uma estratégia transacional pode gerar inconsistência se a publicação falhar.
- Definir exchange, fila e routing key sem uma convenção global pode criar retrabalho futuro.
- Permitir escolha de papéis no cadastro público pode ser inadequado para produção, especialmente para `ADMIN`.
- Política de senha fraca pode comprometer a segurança do serviço.
- Falhas de validação inconsistentes podem dificultar uso por clientes externos.
- Ausência de autenticação nesta versão limita o uso seguro de operações futuras.
- Sem idempotência ou outbox, retries de criação e publicação podem exigir cuidado em versões futuras.
