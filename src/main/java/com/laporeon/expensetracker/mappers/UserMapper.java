package com.laporeon.expensetracker.mappers;

import com.laporeon.expensetracker.dtos.request.RegisterRequestDTO;
import com.laporeon.expensetracker.dtos.response.UserResponseDTO;
import com.laporeon.expensetracker.entities.User;
import com.laporeon.expensetracker.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    public User toEntity(RegisterRequestDTO dto) {
        Instant now = Instant.now();

        return User.builder()
                   .name(dto.name())
                   .email(dto.email())
                   .password(passwordEncoder.encode(dto.password()))
                   .role(Role.USER)
                   .active(true)
                   .createdAt(now)
                   .updatedAt(now)
                   .lastAccessedAt(now)
                   .build();
    }

    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastAccessedAt()
        );
    }

}
