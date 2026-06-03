package com.laporeon.expensetracker.services;

import com.laporeon.expensetracker.dtos.request.LoginRequestDTO;
import com.laporeon.expensetracker.dtos.request.RegisterRequestDTO;
import com.laporeon.expensetracker.dtos.response.LoginResponseDTO;
import com.laporeon.expensetracker.dtos.response.RegisterResponseDTO;
import com.laporeon.expensetracker.dtos.response.UserResponseDTO;
import com.laporeon.expensetracker.entities.User;
import com.laporeon.expensetracker.enums.Role;
import com.laporeon.expensetracker.exceptions.AlreadyRegisteredException;
import com.laporeon.expensetracker.helpers.JwtTokenProvider;
import com.laporeon.expensetracker.helpers.SecurityUtils;
import com.laporeon.expensetracker.mappers.UserMapper;
import com.laporeon.expensetracker.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
public class AuthServiceTest {


    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Authentication authenticationResult;

    @InjectMocks
    private AuthService authService;

    private User mockedUserEntity;
    private UserResponseDTO mockedUserResponseDTO;
    private MockedStatic<SecurityUtils> mockedSecurity;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        mockedUserEntity = User.builder()
                               .name("John Doe")
                               .email("johndoe@gmail.com")
                               .password("$2a$10$encodedPasswordHash")
                               .role(Role.USER)
                               .active(true)
                               .createdAt(Instant.now())
                               .updatedAt(Instant.now())
                               .lastAccessedAt(Instant.now())
                               .build();

        mockedUserResponseDTO = new UserResponseDTO(
                userId,
                "John Doe",
                "johndoe@gmail.com",
                Role.USER,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        mockedSecurity = mockStatic(SecurityUtils.class);
        mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
    }

    @Test
    @DisplayName("Should register User successfully when given valid request data")
    void shouldRegisterUserSuccessfullyWhenGivenValidRequestData() {
        RegisterRequestDTO request = new RegisterRequestDTO("John Doe", "johndoe@gmail.com", "#P4ssword_");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mockedUserEntity);
        when(userRepository.save(any(User.class))).thenReturn(mockedUserEntity);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        when(userMapper.toResponseDTO(mockedUserEntity)).thenReturn(mockedUserResponseDTO);

        RegisterResponseDTO response = authService.register(request);

        assertThat(response.token()).isNotNull();
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.type()).doesNotEndWith(" ");
        assertThat(response.user()).isNotNull();

        verify(userRepository).existsByEmail(request.email());
        verify(userMapper).toEntity(request);
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(any(User.class));
        verify(userMapper).toResponseDTO(mockedUserEntity);

    }

    @Test
    @DisplayName("Should throw AlreadyRegisteredException when email already exists")
    void shouldThrowAlreadyRegisteredExceptionWhenEmailAlreadyExists() {
        RegisterRequestDTO request = new RegisterRequestDTO("John Doe", "johndoe@gmail.com", "#P4ssword_");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);
        assertThrows(AlreadyRegisteredException.class, () -> authService.register(request));

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should return token and update lastAccessedAt when login credentials are valid")
    void shouldReturnTokenAndUpdateLastAccessedAtWhenLoginCredentialsAreValid() {
        LoginRequestDTO request = new LoginRequestDTO("johndoe@gmail.com", "#P4ssword_");
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken("johndoe@gmail.com", "#P4ssword_");

        when(authenticationManager.authenticate(authToken)).thenReturn(authenticationResult);
        when(authenticationResult.getPrincipal()).thenReturn(mockedUserEntity);
        when(jwtTokenProvider.generateToken(mockedUserEntity)).thenReturn("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        when(userRepository.save(any(User.class))).thenReturn(mockedUserEntity);
        when(userMapper.toResponseDTO(mockedUserEntity)).thenReturn(mockedUserResponseDTO);

        LoginResponseDTO response = authService.login(request);

        assertThat(response.token()).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        assertThat(response.type()).isEqualTo("Bearer");

        verify(authenticationManager).authenticate(authToken);
        verify(userRepository).save(argThat(user -> user.getLastAccessedAt() != null));
        verify(jwtTokenProvider).generateToken(mockedUserEntity);
        verify(userMapper).toResponseDTO(mockedUserEntity);
    }

}
