create table outbox_events (
    id uuid primary key,
    aggregate_type varchar(100) not null,
    aggregate_id uuid not null,
    event_type varchar(100) not null,
    event_id uuid not null,
    payload jsonb not null,
    status varchar(30) not null,
    attempts integer not null default 0,
    last_error text,
    next_attempt_at timestamp not null,
    created_at timestamp not null,
    published_at timestamp,
    updated_at timestamp not null,
    constraint uk_outbox_events_event_id unique (event_id),
    constraint ck_outbox_events_status check (status in ('PENDING', 'PUBLISHED', 'FAILED')),
    constraint ck_outbox_events_attempts_non_negative check (attempts >= 0)
);

create index idx_outbox_events_status_next_attempt_at on outbox_events (status, next_attempt_at);
create index idx_outbox_events_aggregate_type_aggregate_id on outbox_events (aggregate_type, aggregate_id);
