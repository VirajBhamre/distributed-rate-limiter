package com.distributed.ratelimiter.diagnostics;

import com.distributed.ratelimiter.config.AppDiagnosticsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Records a high-resolution timestamp when request-path timing is enabled. Placed at the same
 * {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} bucket as Spring Boot’s encoding
 * filter; together with {@link com.distributed.ratelimiter.rateLimiter.RateLimitFilter} at
 * {@code HIGHEST_PRECEDENCE + 1}, the delta approximates work in same-precedence filters plus any
 * filters strictly between them.
 */
public final class RequestInboundTimingFilter extends OncePerRequestFilter {

	public static final String ATTR_FIRST_APP_FILTER_NANO = RequestInboundTimingFilter.class.getName()
			+ ".firstAppFilterNano";

	private final AppDiagnosticsProperties diagnostics;

	public RequestInboundTimingFilter(AppDiagnosticsProperties diagnostics) {
		this.diagnostics = diagnostics;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/actuator/health") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}
		if (diagnostics.requestPathTiming()) {
			request.setAttribute(ATTR_FIRST_APP_FILTER_NANO, System.nanoTime());
		}
		filterChain.doFilter(request, response);
	}
}
