package com.distributed.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional profiling switches. Defaults keep overhead off in production.
 */
@ConfigurationProperties(prefix = "app.diagnostics")
public class AppDiagnosticsProperties {

	private boolean logServletFiltersAtStartup = false;

	private boolean requestPathTiming = false;

	private long requestPathTimingThresholdMs = 25L;

	private boolean securityDownstreamTiming = false;

	private long securityDownstreamTimingThresholdMs = 25L;

	public boolean logServletFiltersAtStartup() {
		return logServletFiltersAtStartup;
	}

	public void setLogServletFiltersAtStartup(boolean logServletFiltersAtStartup) {
		this.logServletFiltersAtStartup = logServletFiltersAtStartup;
	}

	public boolean requestPathTiming() {
		return requestPathTiming;
	}

	public void setRequestPathTiming(boolean requestPathTiming) {
		this.requestPathTiming = requestPathTiming;
	}

	public long requestPathTimingThresholdMs() {
		return requestPathTimingThresholdMs;
	}

	public void setRequestPathTimingThresholdMs(long requestPathTimingThresholdMs) {
		this.requestPathTimingThresholdMs = Math.max(0, requestPathTimingThresholdMs);
	}

	public boolean securityDownstreamTiming() {
		return securityDownstreamTiming;
	}

	public void setSecurityDownstreamTiming(boolean securityDownstreamTiming) {
		this.securityDownstreamTiming = securityDownstreamTiming;
	}

	public long securityDownstreamTimingThresholdMs() {
		return securityDownstreamTimingThresholdMs;
	}

	public void setSecurityDownstreamTimingThresholdMs(long securityDownstreamTimingThresholdMs) {
		this.securityDownstreamTimingThresholdMs = Math.max(0, securityDownstreamTimingThresholdMs);
	}
}
