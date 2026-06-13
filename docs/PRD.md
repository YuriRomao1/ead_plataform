# PRD: EAD Platform

## 1. Visão geral

A EAD Platform é uma plataforma de ensino online para cadastro de usuários, criação e publicação de cursos, matrícula de alunos e registro de notificações geradas por eventos importantes do produto.

O produto também funciona como um MVP técnico para praticar arquitetura de microsserviços em Java, com separação de responsabilidades por serviço, banco de dados por microsserviço, comunicação síncrona via REST, comunicação assíncrona via RabbitMQ, eventos de domínio, testes automatizados e evolução documentada.

Este PRD descreve o produto em nível funcional e de negócio. Decisões técnicas detalhadas pertencem ao HLD, ADRs, FDDs e planos de implementação.

## 2. Problema

Plataformas de ensino online precisam gerenciar diferentes fluxos de usuário, conteúdo e comunicação:

- alunos precisam encontrar cursos publicados e se matricular;
- professores precisam criar, organizar e publicar cursos;
- administradores precisam controlar usuários, permissões e conteúdos;
- o sistema precisa reagir a eventos relevantes, como criação de usuário e matrícula, sem acoplar todos os fluxos em uma única aplicação.

O primeiro desafio do produto é entregar um MVP pequeno, coerente e evolutivo, validando os principais fluxos de uma plataforma EAD sem antecipar funcionalidades comerciais ou operacionais mais avançadas.

## 3. Objetivos do produto

- Permitir cadastro inicial de usuários da plataforma.
- Permitir que usuários tenham papéis claros: `STUDENT`, `TEACHER` e `ADMIN`.
- Permitir que professores ou administradores criem e publiquem cursos.
- Permitir que alunos se matriculem em cursos publicados.
- Registrar notificações a partir de eventos relevantes da plataforma.
- Manter limites claros entre Auth/User, Course e Notification.
- Evoluir por entregas pequenas, testadas e documentadas.

## 4. Personas

### Student

Usuário que consome cursos. Precisa criar conta, visualizar cursos publicados, realizar matrícula e acompanhar seu progresso.

### Teacher

Usuário responsável por criar, editar e publicar cursos. Precisa gerenciar conteúdo educacional e acompanhar a disponibilidade dos cursos.

### Admin

Usuário responsável por gerenciar usuários, cursos e permissões gerais da plataforma.

### System

Representa processos automáticos, como publicação e consumo de eventos, registro de notificações e integrações internas entre serviços.

## 5. Escopo do MVP

### Incluído

- Cadastro de usuário.
- Validação de nome, e-mail, senha e papéis.
- Armazenamento de senha apenas como hash.
- Garantia de e-mail único no Auth/User.
- Publicação do evento `UserCreated`.
- Criação e publicação de cursos.
- Regras de permissão para criação de cursos.
- Matrícula de alunos em cursos publicados.
- Prevenção de matrícula ativa duplicada no mesmo curso.
- Publicação do evento `EnrollmentCreated`.
- Registro de notificações derivadas de `UserCreated` e `EnrollmentCreated`.
- Separação entre os contextos Auth/User, Course e Notification.
- Testes automatizados para regras, persistência, HTTP e mensageria quando aplicável.

### Fora de escopo inicial

- Pagamentos e liberação de acesso pago.
- Marketplace de cursos.
- Avaliações, comentários ou certificados.
- Recomendação de cursos.
- Busca avançada.
- Streaming ou hospedagem de vídeos.
- API Gateway definitivo.
- Estratégia final de JWT, refresh token e autenticação entre microsserviços.
- Dashboards finais de métricas, tracing e operação.
- Aplicação web ou mobile completa.

## 6. Requisitos funcionais

### Usuários

- Todo usuário deve possuir nome, e-mail e senha.
- O e-mail deve ser único no Auth/User.
- A senha deve ser persistida apenas como hash.
- Todo usuário deve possuir ao menos um papel.
- Os papéis iniciais são `STUDENT`, `TEACHER` e `ADMIN`.
- Usuários bloqueados não devem autenticar quando o fluxo de autenticação existir.
- A criação de usuário deve gerar o evento `UserCreated`.

### Cursos

- Apenas usuários com papel `TEACHER` ou `ADMIN` devem criar cursos.
- Todo curso deve possuir título e descrição.
- Todo curso deve iniciar como rascunho.
- Apenas cursos publicados devem aceitar matrículas.
- O Course não deve acessar diretamente o banco do Auth/User.

### Matrículas

- Um aluno só deve se matricular em cursos publicados.
- Um aluno não deve possuir duas matrículas ativas no mesmo curso.
- A criação de matrícula deve gerar o evento `EnrollmentCreated`.

### Notificações

- O Notification deve consumir `UserCreated`.
- Ao consumir `UserCreated`, o sistema deve registrar uma notificação de boas-vindas.
- O Notification deve consumir `EnrollmentCreated`.
- Ao consumir `EnrollmentCreated`, o sistema deve registrar uma notificação de matrícula.

## 7. Requisitos não funcionais

- O sistema deve seguir arquitetura de microsserviços.
- Cada microsserviço deve possuir seu próprio banco de dados.
- Nenhum microsserviço deve acessar diretamente o banco de outro microsserviço.
- Comunicação síncrona entre serviços deve ocorrer via REST.
- Comunicação assíncrona entre serviços deve ocorrer via RabbitMQ.
- Eventos devem representar fatos que já aconteceram.
- Eventos não devem transportar senha, hash de senha ou dados sensíveis desnecessários.
- Regras de negócio devem ficar nas camadas de domínio ou aplicação, não em controllers.
- Cada funcionalidade deve possuir testes proporcionais ao seu risco e superfície.
- O produto deve manter documentação rastreável entre PRD, Domain Context, HLD, ADRs, FDDs e planos de implementação.

## 8. Critérios de sucesso do MVP

- Um aluno consegue ser cadastrado com dados válidos.
- O sistema rejeita cadastro com dados inválidos ou e-mail duplicado.
- Senhas não aparecem em respostas HTTP, eventos ou logs esperados.
- A criação de usuário registra e publica `UserCreated`.
- Um professor ou administrador consegue criar curso.
- Um curso publicado pode receber matrículas.
- Um curso em rascunho não pode receber matrículas.
- O sistema impede matrícula ativa duplicada do mesmo aluno no mesmo curso.
- A criação de matrícula registra e publica `EnrollmentCreated`.
- O Notification registra notificações a partir dos eventos esperados.
- Os módulos afetados possuem testes automatizados passando.
- A documentação técnica permanece alinhada ao comportamento implementado.

## 9. Estado atual conhecido

- O `auth-user-service` existe como módulo implementado.
- O `auth-user-service` implementa `POST /users`.
- O cadastro de usuários já cobre persistência, validações principais, hash BCrypt, resposta pública sem senha e registro/publicação de `UserCreated` via outbox e RabbitMQ.
- O `auth-user-service` já possui manutenção operacional básica da outbox para reprocessar eventos `FAILED`, limpar eventos `PUBLISHED` antigos e expor métricas por status.
- O `auth-user-service` já possui documentação OpenAPI/Swagger para a API HTTP pública.
- `course-service` ainda está planejado e não existe como módulo de código.
- `notification-service` ainda está planejado e não existe como módulo de código.
- Login, JWT, refresh token, bloqueio/desbloqueio de usuário e consumidores de eventos ainda não fazem parte do estado implementado atual.

## 10. Riscos e dependências

- Ausência de estratégia final de autenticação pode limitar fluxos protegidos.
- Ausência de `course-service` bloqueia validação completa de cursos e matrículas.
- Ausência de `notification-service` bloqueia validação completa dos fluxos de notificação.
- Consumidores de eventos precisam ser idempotentes para lidar com duplicidade de mensagens.
- Contratos REST e eventos precisam evoluir com compatibilidade para evitar quebra entre serviços.
- Operação de retry, dead-letter queue, retenção e reprocessamento de eventos precisa estar documentada antes de uso produtivo.
- Observabilidade distribuída será necessária conforme fluxos entre serviços crescerem.

## 11. Relação com documentos técnicos

- `docs/domain-context.md` define personas, bounded contexts, entidades, regras de negócio e eventos de domínio.
- `docs/hld.md` define a arquitetura técnica de alto nível.
- `docs/decisions/` registra decisões arquiteturais aceitas.
- `docs/fdds/` detalha funcionalidades específicas antes da implementação.
- `docs/implementation-plans/` divide FDDs em tarefas executáveis.
- `docs/commit-summaries.md` registra histórico auditável quando commits são criados.

Novas funcionalidades devem partir deste PRD e do Domain Context, respeitar o HLD e as ADRs aplicáveis, e então ser detalhadas em FDD e plano de implementação antes do código.
