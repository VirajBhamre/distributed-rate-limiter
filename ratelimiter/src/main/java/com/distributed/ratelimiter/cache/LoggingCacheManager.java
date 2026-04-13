package com.distributed.ratelimiter.cache;

import java.util.Collection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class LoggingCacheManager implements CacheManager {

	private final CacheManager delegate;

	public LoggingCacheManager(CacheManager delegate) {
		this.delegate = delegate;
	}

	@Override
	public Cache getCache(String name) {
		Cache cache = delegate.getCache(name);
		if (cache == null) {
			return null;
		}
		return new LoggingCache(cache);
	}

	@Override
	public Collection<String> getCacheNames() {
		return delegate.getCacheNames();
	}
}
