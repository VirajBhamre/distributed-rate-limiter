package com.distributed.ratelimiter.security;

import com.distributed.ratelimiter.config.JwtProperties;
import com.distributed.ratelimiter.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	public static final String CLAIM_USER_ID = "uid";
	public static final String CLAIM_ROLE = "role";

	private final JwtProperties properties;
	private final SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(deriveKeyBytes(properties.secret()));
	}

	private static byte[] deriveKeyBytes(String secret) {
		byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
		if (raw.length >= 32) {
			return raw;
		}
		try {
			return MessageDigest.getInstance("SHA-256").digest(raw);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public String generateToken(User user) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + properties.expirationMs());
		return Jwts.builder()
				.subject(user.getUsername())
				.claim(CLAIM_USER_ID, user.getId().toString())
				.claim(CLAIM_ROLE, user.getRole().name())
				.issuedAt(now)
				.expiration(exp)
				.signWith(key)
				.compact();
	}

	public Optional<Claims> parseAndValidate(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			return Optional.of(claims);
		}
		catch (Exception ex) {
			return Optional.empty();
		}
	}

	public Optional<UUID> extractUserId(String token) {
		return parseAndValidate(token).map(c -> UUID.fromString(c.get(CLAIM_USER_ID, String.class)));
	}
}
