package com.distributed.ratelimiter.rateLimiter;

import com.distributed.ratelimiter.config.AppDiagnosticsProperties;
import com.distributed.ratelimiter.config.AppInstanceProperties;
import com.distributed.ratelimiter.diagnostics.RequestInboundTimingFilter;
import com.distributed.ratelimiter.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	private final RedisDistributedRateLimiter rateLimiter;
	private final JwtService jwtService;
	private final AppInstanceProperties instanceProperties;
	private final AppDiagnosticsProperties diagnostics;

	public RateLimitFilter(RedisDistributedRateLimiter rateLimiter, JwtService jwtService,
			AppInstanceProperties instanceProperties, AppDiagnosticsProperties diagnostics) {
		this.rateLimiter = rateLimiter;
		this.jwtService = jwtService;
		this.instanceProperties = instanceProperties;
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
		long rateLimitFilterStartNs = System.nanoTime();
		Long inboundNs = (Long) request.getAttribute(RequestInboundTimingFilter.ATTR_FIRST_APP_FILTER_NANO);
		Long priorChainMs = inboundNs == null ? null : (rateLimitFilterStartNs - inboundNs) / 1_000_000L;

		String clientIp = resolveClientIp(request);
		String member = request.getRequestURI() + ":" + Thread.currentThread().getId() + ":" + System.nanoTime();

		long ipRedisStart = System.nanoTime();
		boolean ipAllowed = rateLimiter.tryConsumeForIp(clientIp, member);
		long ipRedisMs = (System.nanoTime() - ipRedisStart) / 1_000_000L;

		if (!ipAllowed) {
			log.warn("rate_limit decision=blocked scope=ip ip={} path={}", clientIp, request.getRequestURI());
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType("application/json");
			response.getWriter().write(rateLimitJson("ip"));
			maybeLogRequestPathTiming(request, priorChainMs, ipRedisMs, null, null, "blocked", "ip",
					rateLimitFilterStartNs);
			return;
		}

		long jwtParseStart = System.nanoTime();
		Optional<UUID> userId = extractBearerUserId(request);
		long jwtParseMs = (System.nanoTime() - jwtParseStart) / 1_000_000L;

		Long userRedisMs = null;
		if (userId.isPresent()) {
			String userMember = member + ":user";
			long userRedisStart = System.nanoTime();
			boolean userAllowed = rateLimiter.tryConsumeForUser(userId.get(), userMember);
			userRedisMs = (System.nanoTime() - userRedisStart) / 1_000_000L;
			if (!userAllowed) {
				log.warn("rate_limit decision=blocked scope=user userId={} ip={} path={}", userId.get(), clientIp,
						request.getRequestURI());
				response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				response.setContentType("application/json");
				response.getWriter().write(rateLimitJson("user"));
				maybeLogRequestPathTiming(request, priorChainMs, ipRedisMs, jwtParseMs, userRedisMs, "blocked", "user",
						rateLimitFilterStartNs);
				return;
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("rate_limit decision=allowed ip={} path={} userScope={}", clientIp, request.getRequestURI(),
					userId.isPresent());
		}
		maybeLogRequestPathTiming(request, priorChainMs, ipRedisMs, jwtParseMs, userRedisMs, "allowed", null,
				rateLimitFilterStartNs);
		filterChain.doFilter(request, response);
	}

	private void maybeLogRequestPathTiming(HttpServletRequest request, Long priorChainMs, long ipRedisMs,
			Long jwtParseMs, Long userRedisMs, String decision, String blockedScope, long rateLimitFilterStartNs) {
		if (!diagnostics.requestPathTiming()) {
			return;
		}
		long workMs = (System.nanoTime() - rateLimitFilterStartNs) / 1_000_000L;
		if (workMs < diagnostics.requestPathTimingThresholdMs()) {
			return;
		}
		log.warn(
				"diagnostics scope=rate_limit_filter path={} method={} decision={} blockedScope={} priorToRateLimitEntryMs={} ipRedisMs={} jwtParseMs={} userRedisMs={} rateLimitFilterWorkMs={}",
				request.getRequestURI(), request.getMethod(), decision, blockedScope, priorChainMs, ipRedisMs,
				jwtParseMs, userRedisMs, workMs);
	}

	private Optional<UUID> extractBearerUserId(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			return Optional.empty();
		}
		String token = header.substring(7).trim();
		if (token.isEmpty()) {
			return Optional.empty();
		}
		return jwtService.extractUserId(token);
	}

	private String rateLimitJson(String scope) {
		return "{\"error\":\"rate_limit_exceeded\",\"scope\":\"" + scope + "\",\"instanceId\":\""
				+ instanceProperties.id().replace("\"", "\\\"") + "\"}";
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
