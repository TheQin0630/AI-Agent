package com.example.contractagent.user;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.security.JwtService;
import com.example.contractagent.user.dto.AuthResponse;
import com.example.contractagent.user.dto.LoginRequest;
import com.example.contractagent.user.dto.RegisterRequest;
import com.example.contractagent.user.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        JwtService jwt = new JwtService("dev-secret-key-must-be-at-least-32-bytes-long-yes", 86400);
        userService = new UserService(userRepository, encoder, jwt, new UserMapper());
    }

    @Test
    void register_insertsAndReturnsToken() {
        when(userRepository.findByUsername("u1")).thenReturn(Optional.empty());
        AuthResponse res = userService.register(new RegisterRequest("u1", "secret123"));
        assertNotNull(res.token());
        assertEquals("u1", res.user().username());
        assertEquals("USER", res.user().role());
        verify(userRepository).insert(any(User.class));
    }

    @Test
    void register_duplicateUsernameThrows() {
        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(new User()));
        assertThrows(BusinessException.class, () -> userService.register(new RegisterRequest("u1", "secret123")));
    }

    @Test
    void login_wrongPasswordThrows() {
        User u = new User(); u.setId(1L); u.setUsername("u1");
        u.setPasswordHash(new BCryptPasswordEncoder().encode("right"));
        u.setRole(UserRole.USER); u.setEnabled(true);
        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(u));
        assertThrows(BusinessException.class, () -> userService.login(new LoginRequest("u1", "wrong")));
    }
}
