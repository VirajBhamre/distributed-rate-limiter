package com.distributed.ratelimiter.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class InstanceFilterRegistration {

	@Bean
	public FilterRegistrationBean<InstanceIdHeaderFilter> instanceIdHeaderFilter(AppInstanceProperties props) {
		FilterRegistrationBean<InstanceIdHeaderFilter> reg = new FilterRegistrationBean<>();
		reg.setFilter(new InstanceIdHeaderFilter(props));
		reg.addUrlPatterns("/*");
		// After rate limiter (HIGHEST_PRECEDENCE + 1).
		reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
		return reg;
	}
}
