create table users (
    id uuid primary key,
    name varchar not null,
    email varchar not null unique,
    password_hash varchar not null,
    status varchar not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint ck_users_status check (status in ('ACTIVE', 'BLOCKED'))
);

create table user_roles (
    user_id uuid not null,
    role varchar not null,
    constraint pk_user_roles primary key (user_id, role),
    constraint fk_user_roles_user foreign key (user_id) references users (id),
    constraint ck_user_roles_role check (role in ('STUDENT', 'TEACHER', 'ADMIN'))
);

create index idx_users_email on users (email);
create index idx_user_roles_user_id on user_roles (user_id);
