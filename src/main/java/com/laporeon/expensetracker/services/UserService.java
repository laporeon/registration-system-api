package com.laporeon.expensetracker.services;

import com.laporeon.expensetracker.dtos.request.UpdateUserRequestDTO;
import com.laporeon.expensetracker.dtos.response.UserResponseDTO;
import com.laporeon.expensetracker.entities.User;
import com.laporeon.expensetracker.exceptions.AlreadyRegisteredException;
import com.laporeon.expensetracker.exceptions.ResourceNotFoundException;
import com.laporeon.expensetracker.mappers.UserMapper;
import com.laporeon.expensetracker.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDTO update(UUID id, UpdateUserRequestDTO dto) {
        User user = userRepository.findByIdAndActiveTrue(id)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found or inactive"));

        if (userRepository.existsByEmail(dto.email())) {
            throw new AlreadyRegisteredException("Email already registered");
        }

        if (dto.name() != null) user.setName(dto.name());
        if (dto.email() != null) user.setEmail(dto.email());
        if (dto.password() != null) user.setPassword(passwordEncoder.encode(dto.password()));

        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        return userMapper.toResponseDTO(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findByIdAndActiveTrue(id)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found or inactive"));

        user.setActive(false);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
    }

}
