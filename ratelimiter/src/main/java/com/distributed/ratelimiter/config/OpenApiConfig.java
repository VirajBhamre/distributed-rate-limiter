package com.distributed.ratelimiter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	public static final String BEARER_JWT_SCHEME = "bearer-jwt";

	@Bean
	public OpenAPI distributedRateLimiterOpenApi() {
		return new OpenAPI()
				.servers(List.of(new Server().url("/").description(
						"Same origin as this page — use the URL that loaded Swagger (e.g. :8090 behind NGINX), not bare localhost.")))
				.info(new Info().title("Distributed Rate Limiter API").description(
						"JWT auth, Redis-backed rate limits, and Redis cache. Use **Authorize** with `Bearer <token>` from `POST /auth/login`. "
								+ "Successful JSON responses are wrapped as `{ \"instanceId\", \"data\" }`; check response header **X-Instance-Id** as well.")
						.version("v1"))
				.components(new Components().addSecuritySchemes(BEARER_JWT_SCHEME,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
								.description("Use the token from POST /auth/login (Swagger sends it as Bearer).")));
	}

	/** SpringDoc may add an inferred host (e.g. http://localhost); keep only same-origin "/" for Try it out. */
	@Bean
	public OpenApiCustomizer relativeServerOnly() {
		return openApi -> openApi.setServers(List.of(new Server().url("/").description("Current origin (preserve port)")));
	}
}
