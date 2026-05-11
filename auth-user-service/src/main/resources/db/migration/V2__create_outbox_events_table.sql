create table outbox_events (
    event_id uuid primary key,
    event_type varchar not null,
    occurred_at timestamp not null,
    payload jsonb not null,
    status varchar not null default 'PENDING',
    attempts integer not null default 0,
    last_error text,
    created_at timestamp not null,
    updated_at timestamp not null,
    published_at timestamp,
    next_attempt_at timestamp,
    constraint ck_outbox_events_status check (status in ('PENDING', 'PUBLISHED', 'FAILED')),
    constraint ck_outbox_events_attempts check (attempts >= 0)
);

create index idx_outbox_events_status_next_attempt_at on outbox_events (status, next_attempt_at);
create index idx_outbox_events_event_type on outbox_events (event_type);
