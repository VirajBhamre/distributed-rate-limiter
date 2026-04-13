package com.distributed.ratelimiter.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.distributed.ratelimiter.controller")
public class InstanceIdResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	private final AppInstanceProperties instanceProperties;

	public InstanceIdResponseBodyAdvice(AppInstanceProperties instanceProperties) {
		this.instanceProperties = instanceProperties;
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		String path = request.getURI().getPath();
		if (shouldPassThrough(path, selectedContentType, body)) {
			return body;
		}
		if (body == null) {
			Map<String, Object> envelope = new LinkedHashMap<>();
			envelope.put("instanceId", instanceProperties.id());
			envelope.put("data", null);
			return envelope;
		}
		Map<String, Object> envelope = new LinkedHashMap<>();
		envelope.put("instanceId", instanceProperties.id());
		envelope.put("data", body);
		return envelope;
	}

	private boolean shouldPassThrough(String path, MediaType selectedContentType, @Nullable Object body) {
		if (path.startsWith("/actuator")) {
			return true;
		}
		if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
			return true;
		}
		if (selectedContentType != null && !MediaType.APPLICATION_JSON.isCompatibleWith(selectedContentType)) {
			return true;
		}
		if (body == null) {
			return false;
		}
		return body instanceof CharSequence;
	}
}
