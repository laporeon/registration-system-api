package com.laporeon.expensetracker.services;

import com.laporeon.expensetracker.dtos.request.LoginRequestDTO;
import com.laporeon.expensetracker.dtos.request.RegisterRequestDTO;
import com.laporeon.expensetracker.dtos.response.LoginResponseDTO;
import com.laporeon.expensetracker.dtos.response.RegisterResponseDTO;
import com.laporeon.expensetracker.entities.User;
import com.laporeon.expensetracker.exceptions.AlreadyRegisteredException;
import com.laporeon.expensetracker.helpers.JwtTokenProvider;
import com.laporeon.expensetracker.mappers.UserMapper;
import com.laporeon.expensetracker.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new AlreadyRegisteredException("Email already registered");
        }

        User user = userMapper.toEntity(dto);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);

        return new RegisterResponseDTO(
                token,
                "Bearer",
                userMapper.toResponseDTO(user)
        );
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto) {
        var loginPassword = new UsernamePasswordAuthenticationToken(dto.email(), dto.password());
        User user = (User) authenticationManager.authenticate(loginPassword).getPrincipal();

        String token = jwtTokenProvider.generateToken(user);

        user.setLastAccessedAt(Instant.now());
        userRepository.save(user);

        return new LoginResponseDTO(
                token,
                "Bearer",
                userMapper.toResponseDTO(user)
        );
    }

}
