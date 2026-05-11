package com.yuriromao.ead.authuser.infrastructure.persistence;

public enum OutboxEventStatus {

	PENDING,
	PUBLISHED,
	FAILED
}
