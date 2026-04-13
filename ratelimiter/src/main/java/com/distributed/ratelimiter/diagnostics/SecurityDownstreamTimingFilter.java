package com.distributed.ratelimiter.diagnostics;

import com.distributed.ratelimiter.config.AppDiagnosticsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First filter in the Spring Security {@code FilterChainProxy} chain. Measures wall time for
 * everything after it: remaining security filters, {@code DispatcherServlet}, controllers, etc.
 * <p>
 * Requests rejected by {@code RateLimitFilter} never reach this filter (they do not pass
 * {@code DelegatingFilterProxy}).
 */
public final class SecurityDownstreamTimingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(SecurityDownstreamTimingFilter.class);

	private final AppDiagnosticsProperties diagnostics;

	public SecurityDownstreamTimingFilter(AppDiagnosticsProperties diagnostics) {
		this.diagnostics = diagnostics;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		if (!diagnostics.securityDownstreamTiming()) {
			filterChain.doFilter(request, response);
			return;
		}
		long t0 = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			long ms = (System.nanoTime() - t0) / 1_000_000L;
			if (ms >= diagnostics.securityDownstreamTimingThresholdMs()) {
				log.warn(
						"diagnostics scope=security_downstream_from_first_filter method={} path={} status={} durationMs={}",
						request.getMethod(), request.getRequestURI(), response.getStatus(), ms);
			}
		}
	}
}
