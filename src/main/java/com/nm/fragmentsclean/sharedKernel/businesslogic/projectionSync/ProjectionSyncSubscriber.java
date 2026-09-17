package com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync;

import java.util.Objects;

public record ProjectionSyncSubscriber(Kind kind, String userId) {
	public enum Kind {
		USER,
		ADMIN
	}

	public ProjectionSyncSubscriber {
		Objects.requireNonNull(kind, "kind");
		if (kind == Kind.USER && (userId == null || userId.isBlank())) {
			throw new IllegalArgumentException("A user projection subscriber requires a user id");
		}
	}

	public static ProjectionSyncSubscriber user(String userId) {
		return new ProjectionSyncSubscriber(Kind.USER, userId);
	}

	public static ProjectionSyncSubscriber admin() {
		return new ProjectionSyncSubscriber(Kind.ADMIN, null);
	}

	public boolean mayReceive(ProjectionSyncEvent event) {
		if (kind == Kind.ADMIN) {
			return event.audience() != ProjectionSyncAudience.USER;
		}
		return switch (event.audience()) {
			case PUBLIC -> true;
			case USER -> userId.equals(event.recipientId());
			case ADMIN -> false;
		};
	}
}
