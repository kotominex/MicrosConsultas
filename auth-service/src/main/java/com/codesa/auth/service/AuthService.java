package com.codesa.auth.service;

import com.codesa.auth.dto.LoginRequest;
import com.codesa.auth.dto.RegisterRequest;
import com.codesa.auth.dto.TokenResponse;
import com.codesa.auth.dto.UserProfileDto;
import com.codesa.auth.exception.EmailAlreadyExistsException;
import com.codesa.auth.exception.InvalidCredentialsException;
import com.codesa.auth.exception.UserNotFoundException;
import com.codesa.auth.model.Role;
import com.codesa.auth.model.User;
import com.codesa.auth.repository.UserRepository;
import com.codesa.auth.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        userRepository.save(user);

        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new TokenResponse(token, jwtProvider.getExpirationMs());
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new TokenResponse(token, jwtProvider.getExpirationMs());
    }

    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return new UserProfileDto(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
