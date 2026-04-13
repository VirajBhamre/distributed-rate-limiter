package com.distributed.ratelimiter.rateLimiter;

import com.distributed.ratelimiter.config.RateLimitProperties;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisDistributedRateLimiter {

	private static final Logger log = LoggerFactory.getLogger(RedisDistributedRateLimiter.class);

	private final StringRedisTemplate redis;
	private final RateLimitProperties props;
	private final DefaultRedisScript<Long> pairScript;

	public RedisDistributedRateLimiter(StringRedisTemplate redis, RateLimitProperties props) {
		this.redis = redis;
		this.props = props;
		this.pairScript = new DefaultRedisScript<>();
		this.pairScript.setLocation(new ClassPathResource("lua/rate_limit_pair.lua"));
		this.pairScript.setResultType(Long.class);
	}

	public boolean tryConsumeForIp(String ip, String slidingMember) {
		String p = props.keyPrefix();
		String tb = p + "tb:ip:" + ip;
		String sw = p + "sw:ip:" + ip;
		return executePair(tb, sw, props.tokenBucket().ip().capacity(), props.tokenBucket().ip().refillPerSecond(),
				props.slidingWindow().ip().windowMs(), props.slidingWindow().ip().maxRequests(), slidingMember);
	}

	public boolean tryConsumeForUser(UUID userId, String slidingMember) {
		String p = props.keyPrefix();
		String id = userId.toString();
		String tb = p + "tb:user:" + id;
		String sw = p + "sw:user:" + id;
		return executePair(tb, sw, props.tokenBucket().user().capacity(), props.tokenBucket().user().refillPerSecond(),
				props.slidingWindow().user().windowMs(), props.slidingWindow().user().maxRequests(), slidingMember);
	}

	private boolean executePair(String tbKey, String swKey, double capacity, double refillPerSecond, long windowMs,
			int swMax, String swMember) {
		long now = System.currentTimeMillis();
		List<String> keys = List.of(tbKey, swKey);
		Long allowed = redis.execute(pairScript, keys, Double.toString(capacity), Double.toString(refillPerSecond),
				Long.toString(now), "1", Long.toString(windowMs), Integer.toString(swMax), swMember);
		boolean ok = allowed != null && allowed == 1L;
		if (!ok) {
			log.debug("rate_limit dimension blocked tbKey={} swKey={}", tbKey, swKey);
		}
		return ok;
	}
}
