package com.distributed.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
		String keyPrefix,
		TokenBucket tokenBucket,
		SlidingWindow slidingWindow) {

	public record TokenBucket(Limits ip, Limits user) {
		public record Limits(double capacity, double refillPerSecond) {
		}
	}

	public record SlidingWindow(WindowLimits ip, WindowLimits user) {
		public record WindowLimits(long windowMs, int maxRequests) {
		}
	}
}
