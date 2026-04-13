package com.distributed.ratelimiter.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(UUID id, String username, String email, String role, Instant createdAt) {
}
