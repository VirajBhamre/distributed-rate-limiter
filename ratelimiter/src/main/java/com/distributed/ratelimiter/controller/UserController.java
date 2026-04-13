package com.distributed.ratelimiter.controller;

import com.distributed.ratelimiter.dto.UserProfileResponse;
import com.distributed.ratelimiter.dto.UserUpdateRequest;
import com.distributed.ratelimiter.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	@Operation(summary = "Current user profile (cached)")
	public UserProfileResponse me(@AuthenticationPrincipal UserDetails principal) {
		return userService.getProfileForCurrentUser(principal.getUsername());
	}

	@GetMapping("/id/{id}")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Profile by user id (self or admin)")
	public UserProfileResponse byId(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
		UserProfileResponse profile = userService.getProfileById(id);
		if (!principal.getUsername().equalsIgnoreCase(profile.username()) && !principal.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		return profile;
	}

	@GetMapping("/{username}")
	@PreAuthorize("authentication.name == #username || hasRole('ADMIN')")
	@Operation(summary = "Profile by username (self or admin)")
	public UserProfileResponse byUsername(@PathVariable String username) {
		return userService.getProfileByUsername(username);
	}

	@PutMapping("/me")
	@Operation(summary = "Update current user (evicts cache)")
	public UserProfileResponse updateMe(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody UserUpdateRequest request) {
		return userService.updateCurrentUser(principal.getUsername(), request);
	}
}
