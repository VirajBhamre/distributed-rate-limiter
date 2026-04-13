package com.distributed.ratelimiter.config;

import com.distributed.ratelimiter.diagnostics.RequestInboundTimingFilter;
import com.distributed.ratelimiter.rateLimiter.RateLimitFilter;
import com.distributed.ratelimiter.rateLimiter.RedisDistributedRateLimiter;
import com.distributed.ratelimiter.security.JwtService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RateLimitFilterConfig {

	@Bean
	public FilterRegistrationBean<RequestInboundTimingFilter> requestInboundTimingFilterRegistration(
			AppDiagnosticsProperties diagnostics) {
		FilterRegistrationBean<RequestInboundTimingFilter> reg = new FilterRegistrationBean<>();
		reg.setFilter(new RequestInboundTimingFilter(diagnostics));
		reg.addUrlPatterns("/*");
		reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return reg;
	}

	@Bean
	public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RedisDistributedRateLimiter rateLimiter,
			JwtService jwtService, AppInstanceProperties instanceProperties, AppDiagnosticsProperties diagnostics) {
		FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>();
		reg.setFilter(new RateLimitFilter(rateLimiter, jwtService, instanceProperties, diagnostics));
		reg.addUrlPatterns("/*");
		// After RequestInboundTimingFilter and the same-precedence bucket as Spring Boot defaults (e.g. encoding).
		reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		return reg;
	}
}
