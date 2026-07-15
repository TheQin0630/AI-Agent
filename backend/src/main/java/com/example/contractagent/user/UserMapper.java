package com.example.contractagent.user;

import com.example.contractagent.user.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getUsername(), u.getRole().name());
    }
}
