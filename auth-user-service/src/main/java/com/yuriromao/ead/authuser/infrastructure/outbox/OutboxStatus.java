package com.yuriromao.ead.authuser.infrastructure.outbox;

/** Snapshot of outbox records grouped by publication status. */
public record OutboxStatus(long pending, long published, long failed) {}
