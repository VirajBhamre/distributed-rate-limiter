package com.distributed.ratelimiter.diagnostics;

import com.distributed.ratelimiter.config.AppDiagnosticsProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs every {@link FilterRegistrationBean} order and URL patterns once the context is ready.
 * Lower order values run earlier on the inbound servlet request.
 */
@Component
public class ServletFilterOrderLogger {

	private static final Logger log = LoggerFactory.getLogger(ServletFilterOrderLogger.class);

	private final AppDiagnosticsProperties diagnostics;

	public ServletFilterOrderLogger(AppDiagnosticsProperties diagnostics) {
		this.diagnostics = diagnostics;
	}

	@EventListener(ApplicationReadyEvent.class)
	@SuppressWarnings("rawtypes")
	public void onReady(ApplicationReadyEvent event) {
		if (!diagnostics.logServletFiltersAtStartup()) {
			return;
		}
		Map<String, FilterRegistrationBean> beans = event.getApplicationContext()
				.getBeansOfType(FilterRegistrationBean.class);

		List<Map.Entry<String, FilterRegistrationBean>> sorted = new ArrayList<>(beans.entrySet());
		sorted.sort(Comparator.comparingInt(e -> e.getValue().getOrder()));

		log.info("diagnostics scope=servlet_filter_registrations count={}", sorted.size());
		for (var e : sorted) {
			FilterRegistrationBean fr = e.getValue();
			log.info("diagnostics servlet_filter beanName={} order={} filterClass={} urlPatterns={}", e.getKey(),
					fr.getOrder(), fr.getFilter().getClass().getName(), fr.getUrlPatterns());
		}
		log.info(
				"diagnostics note=Rejected_at_rate_limit_filter requests never enter springSecurityFilterChain. "
						+ "High 429 latency with low priorServletFilterMs usually means Tomcat worker or connection backlog wait before filters.");
	}
}
