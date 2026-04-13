package com.distributed.ratelimiter.security;

import com.distributed.ratelimiter.dto.CachedUserSummary;
import com.distributed.ratelimiter.service.UserCacheService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DomainUserDetailsService implements UserDetailsService {

	private final UserCacheService userCacheService;

	public DomainUserDetailsService(UserCacheService userCacheService) {
		this.userCacheService = userCacheService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		CachedUserSummary summary = userCacheService.getByUsername(username);
		if (summary == null) {
			throw new UsernameNotFoundException(username);
		}
		return org.springframework.security.core.userdetails.User.withUsername(summary.username())
				.password("{noop}jwt-placeholder")
				.roles(summary.role().name())
				.build();
	}
}
