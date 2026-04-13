package com.distributed.ratelimiter.service;

import com.distributed.ratelimiter.dto.AuthResponse;
import com.distributed.ratelimiter.dto.LoginRequest;
import com.distributed.ratelimiter.dto.UserProfileResponse;
import com.distributed.ratelimiter.entity.User;
import com.distributed.ratelimiter.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

	private final UserService userService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public UserProfileResponse register(String username, String email, String rawPassword) {
		return userService.register(username, email, rawPassword);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		User user = userService.findByUsernameWithPassword(request.username())
				.orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "invalid credentials"));
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new ResponseStatusException(UNAUTHORIZED, "invalid credentials");
		}
		String token = jwtService.generateToken(user);
		return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole().name());
	}
}
