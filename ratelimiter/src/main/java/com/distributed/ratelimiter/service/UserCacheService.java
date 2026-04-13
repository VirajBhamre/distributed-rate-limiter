package com.distributed.ratelimiter.service;

import com.distributed.ratelimiter.dto.CachedUserSummary;
import com.distributed.ratelimiter.repository.UserRepository;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCacheService {

	private final UserRepository userRepository;

	public UserCacheService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Cacheable(cacheNames = "userByUsername", key = "#username", unless = "#result == null")
	@Transactional(readOnly = true)
	public CachedUserSummary getByUsername(String username) {
		return userRepository.findByUsername(username).map(CachedUserSummary::fromEntity).orElse(null);
	}

	@Cacheable(cacheNames = "userById", key = "#id", unless = "#result == null")
	@Transactional(readOnly = true)
	public CachedUserSummary getById(UUID id) {
		return userRepository.findById(id).map(CachedUserSummary::fromEntity).orElse(null);
	}
}
