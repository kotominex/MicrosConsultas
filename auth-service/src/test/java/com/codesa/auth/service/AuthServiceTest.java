package com.codesa.auth.service;

import com.codesa.auth.dto.LoginRequest;
import com.codesa.auth.dto.RegisterRequest;
import com.codesa.auth.dto.TokenResponse;
import com.codesa.auth.exception.EmailAlreadyExistsException;
import com.codesa.auth.exception.InvalidCredentialsException;
import com.codesa.auth.model.Role;
import com.codesa.auth.model.User;
import com.codesa.auth.repository.UserRepository;
import com.codesa.auth.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_deberiaCrearUsuarioYRetornarToken_cuandoEmailNoExiste() {
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        }).when(userRepository).save(any(User.class));

        RegisterRequest request = new RegisterRequest("nuevo@correo.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(jwtProvider.generateToken(anyLong(), anyString(), any(Role.class))).thenReturn("jwt-token");
        when(jwtProvider.getExpirationMs()).thenReturn(3600000L);

        TokenResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.expiresInMs()).isEqualTo(3600000L);
    }

    @Test
    void register_deberiaLanzarExcepcion_cuandoEmailYaExiste() {
        RegisterRequest request = new RegisterRequest("existente@correo.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void login_deberiaLanzarExcepcion_cuandoCredencialesInvalidas() {
        LoginRequest request = new LoginRequest("usuario@correo.com", "wrong-password");
        User user = new User();
        user.setId(1L);
        user.setEmail(request.email());
        user.setPasswordHash("hashed");
        user.setRole(Role.USER);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_deberiaRetornarToken_cuandoCredencialesValidas() {
        LoginRequest request = new LoginRequest("usuario@correo.com", "password123");
        User user = new User();
        user.setId(1L);
        user.setEmail(request.email());
        user.setPasswordHash("hashed");
        user.setRole(Role.USER);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtProvider.generateToken(anyLong(), anyString(), any(Role.class))).thenReturn("jwt-token");
        when(jwtProvider.getExpirationMs()).thenReturn(3600000L);

        TokenResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
    }
}
