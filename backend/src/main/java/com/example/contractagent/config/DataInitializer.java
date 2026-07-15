package com.example.contractagent.config;

import com.example.contractagent.user.User;
import com.example.contractagent.user.UserRepository;
import com.example.contractagent.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 启动时初始化默认管理员账号 admin / admin123。
 * 若 admin 已存在则跳过，不重置密码。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User u = new User();
                u.setUsername("admin");
                u.setPasswordHash(passwordEncoder.encode("admin123"));
                u.setRole(UserRole.ADMIN);
                u.setEnabled(true);
                userRepository.insert(u);
                log.info("已初始化默认管理员账号: admin / admin123");
            }
        };
    }
}
