package com.example.contractagent.user;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.security.JwtService;
import com.example.contractagent.user.dto.AuthResponse;
import com.example.contractagent.user.dto.LoginRequest;
import com.example.contractagent.user.dto.RegisterRequest;
import com.example.contractagent.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "用户名已存在");
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(UserRole.USER);
        u.setEnabled(true);
        userRepository.insert(u);
        return buildAuth(u);
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByUsername(req.username())
                .orElseThrow(() -> BusinessException.of(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!Boolean.TRUE.equals(u.getEnabled())) throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已禁用");
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return buildAuth(u);
    }

    public UserDto me(Long userId) {
        User u = userRepository.selectById(userId);
        if (u == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "用户不存在");
        return userMapper.toDto(u);
    }

    private AuthResponse buildAuth(User u) {
        String token = jwtService.issue(u.getId(), u.getUsername(), u.getRole().name());
        return new AuthResponse(token, jwtService.getTtlSeconds(), userMapper.toDto(u));
    }
}
