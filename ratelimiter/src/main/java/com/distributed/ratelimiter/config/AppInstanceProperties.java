package com.distributed.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.instance")
public record AppInstanceProperties(String id) {

	public AppInstanceProperties {
		if (id == null || id.isBlank()) {
			id = "local";
		}
	}
}
