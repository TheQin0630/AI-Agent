package com.example.contractagent.user;

import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.user.dto.AuthResponse;
import com.example.contractagent.user.dto.LoginRequest;
import com.example.contractagent.user.dto.RegisterRequest;
import com.example.contractagent.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto> register(@RequestBody @Valid RegisterRequest req) {
        return ApiResponse.ok(userService.register(req).user());
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        return ApiResponse.ok(userService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.me(userId));
    }
}
