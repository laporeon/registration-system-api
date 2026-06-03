package com.laporeon.expensetracker.services;

import com.laporeon.expensetracker.dtos.request.CreateExpenseRequestDTO;
import com.laporeon.expensetracker.dtos.request.UpdateExpenseRequestDTO;
import com.laporeon.expensetracker.dtos.response.ExpenseResponseDTO;
import com.laporeon.expensetracker.dtos.response.PageResponseDTO;
import com.laporeon.expensetracker.entities.Expense;
import com.laporeon.expensetracker.enums.Category;
import com.laporeon.expensetracker.exceptions.ResourceNotFoundException;
import com.laporeon.expensetracker.helpers.SecurityUtils;
import com.laporeon.expensetracker.mappers.ExpenseMapper;
import com.laporeon.expensetracker.repositories.ExpenseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Tests")
public class ExpenseServiceTest {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseService expenseService;

    private Expense mockedExpenseEntity;
    private ExpenseResponseDTO mockedExpenseResponse;
    private UUID userId;
    private MockedStatic<SecurityUtils> mockedSecurity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockedExpenseEntity = Expense.builder()
                      .name("Prime Video")
                      .description("Prime Video annual subscription.")
                      .amount(BigDecimal.valueOf(199.90))
                      .category(Category.SUBSCRIPTIONS)
                      .userId(userId)
                      .date(LocalDate.of(2025, 12, 18))
                      .createdAt(Instant.now())
                      .updatedAt(Instant.now())
                      .build();


        mockedExpenseResponse = new ExpenseResponseDTO(
                mockedExpenseEntity.getId(),
                mockedExpenseEntity.getName(),
                mockedExpenseEntity.getDescription(),
                mockedExpenseEntity.getAmount(),
                mockedExpenseEntity.getCategory(),
                mockedExpenseEntity.getDate(),
                mockedExpenseEntity.getCreatedAt(),
                mockedExpenseEntity.getUpdatedAt()
        );

        mockedSecurity = mockStatic(SecurityUtils.class);
        mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
    }

    @Test
    @DisplayName("Should save Expense successfully when given valid request data")
    void shouldSaveExpenseSuccessfullyWhenGivenValidRequestData() {
        CreateExpenseRequestDTO request = new CreateExpenseRequestDTO(
                "Prime Video",
                "Prime Video annual subscription.",
                 BigDecimal.valueOf(199.90),
                Category.SUBSCRIPTIONS.toString(),
                LocalDate.of(2025, 12, 18));

        when(expenseMapper.toEntity(any(CreateExpenseRequestDTO.class), any(UUID.class))).thenReturn(mockedExpenseEntity);
        when(expenseRepository.save(any(Expense.class))).thenReturn(mockedExpenseEntity);
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(mockedExpenseResponse);

        ExpenseResponseDTO response = expenseService.addExpense(request);

        assertThat(response.id()).isEqualTo(mockedExpenseResponse.id());
        assertThat(response.name()).isEqualTo(mockedExpenseResponse.name());
        assertThat(response.createdAt()).isEqualTo(mockedExpenseResponse.createdAt());

        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("Should return page of expenses when given valid pageable")
    void shouldReturnPageOfExpensesWhenGivenValidPageable() {
        Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
        Page<Expense> expectedPage = new PageImpl<>(List.of(mockedExpenseEntity), pageable, 1L);

        PageResponseDTO<ExpenseResponseDTO> expectedResponse = new PageResponseDTO<>(
                List.of(mockedExpenseResponse),
                0,
                10,
                1,
                1,
                1,
                true,
                true,
                false,
                true,
                false
        );

        when(expenseRepository.findAllByUserId(userId, pageable)).thenReturn(expectedPage);
        when(expenseMapper.toPageResponseDTO(expectedPage)).thenReturn(expectedResponse);

        PageResponseDTO<ExpenseResponseDTO> result = expenseService.listAllExpenses(pageable, null, null);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        verify(expenseRepository, times(1)).findAllByUserId(userId, pageable);
    }

    @Test
    @DisplayName("Should use date range query when startDate and endDate are provided")
    void shouldUseDateRangeQueryWhenStartDateAndEndDateAreProvided() {
        Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        Page<Expense> expectedPage = new PageImpl<>(List.of(mockedExpenseEntity), pageable, 1L);

        when(expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate, pageable)).thenReturn(expectedPage);
        when(expenseMapper.toPageResponseDTO(expectedPage)).thenReturn(new PageResponseDTO<>(List.of(mockedExpenseResponse), 0, 10, 1, 1, 1, true, true, false, true, false));

        expenseService.listAllExpenses(pageable, startDate, endDate);

        verify(expenseRepository).findByUserIdAndDateBetween(userId, startDate, endDate, pageable);
        verify(expenseRepository, never()).findByUserIdAndDateGreaterThanEqual(any(), any(), any());
        verify(expenseRepository, never()).findByUserIdAndDateLessThanEqual(any(), any(), any());
        verify(expenseRepository, never()).findAllByUserId(any(), any());
    }

    @Test
    @DisplayName("Should use startDate query when only startDate is provided")
    void shouldUseStartDateQueryWhenOnlyStartDateIsProvided() {
        Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        Page<Expense> expectedPage = new PageImpl<>(List.of(mockedExpenseEntity), pageable, 1L);

        when(expenseRepository.findByUserIdAndDateGreaterThanEqual(userId, startDate, pageable)).thenReturn(expectedPage);
        when(expenseMapper.toPageResponseDTO(expectedPage)).thenReturn(new PageResponseDTO<>(List.of(mockedExpenseResponse), 0, 10, 1, 1, 1, true, true, false, true, false));

        expenseService.listAllExpenses(pageable, startDate, null);

        verify(expenseRepository).findByUserIdAndDateGreaterThanEqual(userId, startDate, pageable);
        verify(expenseRepository, never()).findByUserIdAndDateBetween(any(), any(), any(), any());
        verify(expenseRepository, never()).findByUserIdAndDateLessThanEqual(any(), any(), any());
        verify(expenseRepository, never()).findAllByUserId(any(), any());
    }

    @Test
    @DisplayName("Should use endDate query when only endDate is provided")
    void shouldUseEndDateQueryWhenOnlyEndDateIsProvided() {
        Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        Page<Expense> expectedPage = new PageImpl<>(List.of(mockedExpenseEntity), pageable, 1L);

        when(expenseRepository.findByUserIdAndDateLessThanEqual(userId, endDate, pageable)).thenReturn(expectedPage);
        when(expenseMapper.toPageResponseDTO(expectedPage)).thenReturn(
                new PageResponseDTO<>(List.of(mockedExpenseResponse),
                        0,
                        10,
                        1,
                        1,
                        1,
                        true,
                        true,
                        false,
                        true,
                        false));

        expenseService.listAllExpenses(pageable, null, endDate);

        verify(expenseRepository).findByUserIdAndDateLessThanEqual(userId, endDate, pageable);
        verify(expenseRepository, never()).findByUserIdAndDateBetween(any(), any(), any(), any());
        verify(expenseRepository, never()).findByUserIdAndDateGreaterThanEqual(any(), any(), any());
        verify(expenseRepository, never()).findAllByUserId(any(), any());
    }

    @Test
    @DisplayName("Should return empty page when no expenses exist")
    void shouldReturnEmptyPageWhenNoExpensesExist() {
        Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
        Page<Expense> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        PageResponseDTO<ExpenseResponseDTO> emptyPageResponse = new PageResponseDTO<>(
                List.of(),
                0,
                10,
                1,
                0,
                0,
                true,
                true,
                true,
                true,
                false
        );

        when(expenseRepository.findAllByUserId(userId, pageable)).thenReturn(emptyPage);
        when(expenseMapper.toPageResponseDTO(any(Page.class))).thenReturn(emptyPageResponse);

        PageResponseDTO<ExpenseResponseDTO> result = expenseService.listAllExpenses(pageable, null, null);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.isEmpty()).isTrue();

        verify(expenseRepository, times(1)).findAllByUserId(userId, pageable);
    }

    @Test
    @DisplayName("Should return expense when given existing id")
    void shouldReturnExpenseWhenGivenExistingId() {
        when(expenseRepository.findByIdAndUserId(mockedExpenseEntity.getId(), userId)).thenReturn(Optional.of(mockedExpenseEntity));
        when(expenseMapper.toDTO(any(Expense.class))).thenReturn(mockedExpenseResponse);

        ExpenseResponseDTO sut = expenseService.findExpense(mockedExpenseEntity.getId());

        assertThat(sut.id()).isEqualTo(mockedExpenseResponse.id());
        assertThat(sut.name()).isEqualTo(mockedExpenseResponse.name());

        verify(expenseRepository, times(1)).findByIdAndUserId(mockedExpenseEntity.getId(), userId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when id does not exist")
    void shouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
        UUID invalidId = UUID.randomUUID();

        when(expenseRepository.findByIdAndUserId(invalidId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.findExpense(invalidId));

        verify(expenseRepository, times(1)).findByIdAndUserId(invalidId, userId);
    }

    @Test
    @DisplayName("Should successfully update expense when given existing id and valid request data")
    void shouldUpdateExpenseWhenGivenExistingIdAndValidRequestData() {
        String updatedDescription = "Updated subscription description";
        UpdateExpenseRequestDTO request = new UpdateExpenseRequestDTO(null, updatedDescription, null, null, null);

        when(expenseRepository.findByIdAndUserId(mockedExpenseEntity.getId(), userId)).thenReturn(Optional.of(mockedExpenseEntity));
        when(expenseMapper.toDTO(any(Expense.class))).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            return new ExpenseResponseDTO(
                    expense.getId(),
                    expense.getName(),
                    expense.getDescription(),
                    expense.getAmount(),
                    expense.getCategory(),
                    expense.getDate(),
                    expense.getCreatedAt(),
                    expense.getUpdatedAt()
            );
        });
        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        ExpenseResponseDTO response = expenseService.updateExpense(mockedExpenseEntity.getId(), request);
        ArgumentCaptor<Expense> savedExpenseCaptor = ArgumentCaptor.forClass(Expense.class);

        verify(expenseRepository, times(1)).findByIdAndUserId(mockedExpenseEntity.getId(), userId);
        verify(expenseRepository, times(1)).save(savedExpenseCaptor.capture());

        Expense savedExpense = savedExpenseCaptor.getValue();
        assertThat(savedExpense.getDescription()).isEqualTo(updatedDescription);

        assertThat(response.id()).isEqualTo(savedExpense.getId());
        assertThat(response.description()).isEqualTo(updatedDescription);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating expense with non existing id")
    void shouldThrowResourceNotFoundExceptionWhenUpdatingExpenseWithNonExistingId() {
        UUID invalidId = UUID.randomUUID();
        String updatedDescription = "Updated subscription description";
        UpdateExpenseRequestDTO request = new UpdateExpenseRequestDTO(null, updatedDescription, null, null, null);

        when(expenseRepository.findByIdAndUserId(invalidId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.updateExpense(invalidId, request));

        verify(expenseRepository, times(1)).findByIdAndUserId(invalidId, userId);
    }

    @Test
    @DisplayName("Should delete expense when given existing id")
    void shouldDeleteExpenseWhenGivenExistingId() {
        when(expenseRepository.findByIdAndUserId(mockedExpenseEntity.getId(), userId)).thenReturn(Optional.of(mockedExpenseEntity));

        doNothing().when(expenseRepository).delete(mockedExpenseEntity);

        expenseService.deleteExpense(mockedExpenseEntity.getId());

        verify(expenseRepository, times(1)).findByIdAndUserId(mockedExpenseEntity.getId(), userId);
        verify(expenseRepository, times(1)).delete(mockedExpenseEntity);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting expense with non existing id")
    void shouldThrowResourceNotFoundExceptionWhenDeletingExpenseWithNonExistingId() {
        UUID invalidId = UUID.randomUUID();

        when(expenseRepository.findByIdAndUserId(invalidId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.deleteExpense(invalidId));

        verify(expenseRepository, times(1)).findByIdAndUserId(invalidId, userId);
        verify(expenseRepository, never()).delete(any(Expense.class));
    }
}
