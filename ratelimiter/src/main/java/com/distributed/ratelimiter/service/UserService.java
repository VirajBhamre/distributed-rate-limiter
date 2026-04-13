package com.distributed.ratelimiter.service;

import com.distributed.ratelimiter.dto.CachedUserSummary;
import com.distributed.ratelimiter.dto.UserProfileResponse;
import com.distributed.ratelimiter.dto.UserUpdateRequest;
import com.distributed.ratelimiter.entity.Role;
import com.distributed.ratelimiter.entity.User;
import com.distributed.ratelimiter.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserCacheService userCacheService;
	private final CacheManager cacheManager;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserCacheService userCacheService,
			CacheManager cacheManager) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.userCacheService = userCacheService;
		this.cacheManager = cacheManager;
	}

	@Transactional
	public UserProfileResponse register(String username, String email, String rawPassword) {
		if (userRepository.existsByUsername(username)) {
			throw new ResponseStatusException(CONFLICT, "username already exists");
		}
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(CONFLICT, "email already exists");
		}
		User user = new User(username, email, passwordEncoder.encode(rawPassword), Role.USER, Instant.now());
		User saved = userRepository.save(user);
		return toProfile(saved);
	}

	@Transactional(readOnly = true)
	public Optional<User> findByUsernameWithPassword(String username) {
		return userRepository.findByUsername(username);
	}

	@Transactional(readOnly = true)
	public Optional<CachedUserSummary> findSummaryByUsername(String username) {
		return Optional.ofNullable(userCacheService.getByUsername(username));
	}

	@Transactional(readOnly = true)
	public Optional<CachedUserSummary> findSummaryById(UUID id) {
		return Optional.ofNullable(userCacheService.getById(id));
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getProfileByUsername(String username) {
		CachedUserSummary s = userCacheService.getByUsername(username);
		if (s == null) {
			throw new ResponseStatusException(NOT_FOUND);
		}
		return toProfile(s);
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getProfileById(UUID id) {
		CachedUserSummary s = userCacheService.getById(id);
		if (s == null) {
			throw new ResponseStatusException(NOT_FOUND);
		}
		return toProfile(s);
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getProfileForCurrentUser(String username) {
		return getProfileByUsername(username);
	}

	@Transactional
	public UserProfileResponse updateCurrentUser(String username, UserUpdateRequest request) {
		User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
		evictUserCaches(user);
		if (request.email() != null && !request.email().isBlank()) {
			if (userRepository.existsByEmail(request.email()) && !request.email().equalsIgnoreCase(user.getEmail())) {
				throw new ResponseStatusException(CONFLICT, "email already exists");
			}
			user.setEmail(request.email());
		}
		if (request.newPassword() != null && !request.newPassword().isBlank()) {
			if (request.currentPassword() == null || request.currentPassword().isBlank()) {
				throw new ResponseStatusException(FORBIDDEN, "current password required");
			}
			if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
				throw new ResponseStatusException(FORBIDDEN, "invalid current password");
			}
			user.setPassword(passwordEncoder.encode(request.newPassword()));
		}
		User saved = userRepository.save(user);
		return toProfile(saved);
	}

	private void evictUserCaches(User user) {
		var byId = cacheManager.getCache("userById");
		if (byId != null) {
			byId.evict(user.getId());
		}
		var byName = cacheManager.getCache("userByUsername");
		if (byName != null) {
			byName.evict(user.getUsername());
		}
	}

	private UserProfileResponse toProfile(CachedUserSummary s) {
		return new UserProfileResponse(s.id(), s.username(), s.email(), s.role().name(), s.createdAt());
	}

	private UserProfileResponse toProfile(User u) {
		return new UserProfileResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(), u.getCreatedAt());
	}
}
