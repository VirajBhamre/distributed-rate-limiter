package com.distributed.ratelimiter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
		@Size(max = 255) @Email String email,
		@Size(min = 8, max = 128) String newPassword,
		String currentPassword) {
}
