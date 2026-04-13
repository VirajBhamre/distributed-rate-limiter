package com.distributed.ratelimiter.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
public record AppCacheProperties(Duration userByIdTtl, Duration userByUsernameTtl) {
}
