package com.distributed.ratelimiter.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sets {@value #HEADER_NAME} on every response so clients can see which app replica handled the request
 * (works for errors and rate limits too).
 */
public class InstanceIdHeaderFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Instance-Id";

	private final AppInstanceProperties instanceProperties;

	public InstanceIdHeaderFilter(AppInstanceProperties instanceProperties) {
		this.instanceProperties = instanceProperties;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		response.setHeader(HEADER_NAME, instanceProperties.id());
		filterChain.doFilter(request, response);
	}
}
