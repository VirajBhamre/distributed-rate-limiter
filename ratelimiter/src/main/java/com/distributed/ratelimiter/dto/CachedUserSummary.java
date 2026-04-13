package com.distributed.ratelimiter.dto;

import com.distributed.ratelimiter.entity.Role;
import com.distributed.ratelimiter.entity.User;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record CachedUserSummary(UUID id, String username, String email, Role role, Instant createdAt)
		implements Serializable {

	private static final long serialVersionUID = 1L;

	public static CachedUserSummary fromEntity(User u) {
		return new CachedUserSummary(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getCreatedAt());
	}
}
