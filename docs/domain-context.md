# Domain Context — EAD Platform

## 1. Visão geral

A EAD Platform é uma plataforma de ensino online construída em Java com arquitetura de microsserviços. O sistema permite o cadastro de usuários, autenticação, criação e publicação de cursos, matrícula de alunos e envio de notificações assíncronas para eventos importantes da plataforma.

O objetivo inicial é construir um MVP técnico que demonstre os principais conceitos de microsserviços:

- separação de responsabilidades por serviço;
- banco de dados por microsserviço;
- comunicação síncrona via HTTP;
- comunicação assíncrona via mensageria;
- publicação e consumo de eventos;
- observabilidade mínima;
- histórico profissional de evolução via commits.

---

## 2. Personas

### Student

Usuário que consome os cursos. Pode visualizar cursos publicados, realizar matrícula e acompanhar seu progresso.

### Teacher

Usuário responsável por criar, editar e publicar cursos.

### Admin

Usuário responsável por gerenciar usuários, cursos e permissões gerais da plataforma.

### System

Representa processos automáticos da plataforma, como envio de notificações e consumo de eventos.

---

## 3. Bounded Contexts

### Auth/User

Responsável por usuários, autenticação, autorização e publicação de eventos relacionados a usuários.

### Course

Responsável por cursos, módulos, aulas e matrículas.

### Notification

Responsável por consumir eventos e enviar notificações.

### Payment

Contexto futuro responsável por pagamentos e liberação de acesso pago.

---

## 4. Entidades principais

### User

Representa um usuário da plataforma.

Atributos:

- id
- name
- email
- passwordHash
- status
- roles
- createdAt
- updatedAt

### Course

Representa um curso publicado ou em rascunho.

Atributos:

- id
- title
- description
- status
- teacherId
- createdAt
- updatedAt

### Enrollment

Representa a matrícula de um aluno em um curso.

Atributos:

- id
- studentId
- courseId
- status
- createdAt

### Notification

Representa uma notificação enviada ou pendente.

Atributos:

- id
- recipient
- type
- subject
- content
- status
- createdAt

---

## 5. Regras de negócio

### Usuários

RN-001: Todo usuário deve possuir nome, e-mail e senha.

RN-002: O e-mail do usuário deve ser único dentro do Auth/User Service.

RN-003: A senha do usuário deve ser armazenada apenas como hash.

RN-004: Todo usuário deve possuir ao menos um papel.

RN-005: Usuários podem ter os papéis STUDENT, TEACHER ou ADMIN.

RN-006: Usuários bloqueados não podem autenticar.

RN-007: Ao criar um usuário, o Auth/User Service deve publicar o evento UserCreated.

---

### Cursos

RN-008: Apenas usuários com papel TEACHER ou ADMIN podem criar cursos.

RN-009: Todo curso começa com status DRAFT.

RN-010: Apenas cursos com status PUBLISHED podem receber matrículas.

RN-011: Um curso deve possuir título e descrição.

RN-012: O Course Service não pode acessar diretamente o banco do Auth/User Service.

---

### Matrículas

RN-013: Um aluno só pode se matricular em cursos publicados.

RN-014: Um aluno não pode ter duas matrículas ativas no mesmo curso.

RN-015: Ao criar uma matrícula, o Course Service deve publicar o evento EnrollmentCreated.

---

### Notificações

RN-016: O Notification Service deve consumir o evento UserCreated.

RN-017: Ao consumir UserCreated, o Notification Service deve registrar uma notificação de boas-vindas.

RN-018: O Notification Service deve consumir o evento EnrollmentCreated.

RN-019: Ao consumir EnrollmentCreated, o Notification Service deve registrar uma notificação de matrícula.

---

### Arquitetura

RN-020: Cada microsserviço deve possuir seu próprio banco de dados.

RN-021: Nenhum microsserviço pode acessar diretamente o banco de outro microsserviço.

RN-022: Comunicação síncrona entre microsserviços deve ocorrer via HTTP.

RN-023: Comunicação assíncrona entre microsserviços deve ocorrer via mensageria.

RN-024: Eventos devem representar fatos que já aconteceram.

RN-025: Comandos devem representar intenção de executar uma ação.

---

## 6. Eventos de domínio

### UserCreated

Publicado quando um novo usuário é criado.

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