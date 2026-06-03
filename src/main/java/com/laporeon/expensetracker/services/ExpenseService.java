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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseMapper expenseMapper;
    private final ExpenseRepository expenseRepository;

    public ExpenseResponseDTO addExpense(CreateExpenseRequestDTO dto) {
        Expense expense = expenseMapper.toEntity(dto, SecurityUtils.getCurrentUserId());

        expenseRepository.save(expense);

        return expenseMapper.toDTO(expense);
    }

    public PageResponseDTO<ExpenseResponseDTO> listAllExpenses(Pageable pageable, LocalDate startDate, LocalDate endDate) {
        Page<Expense> expensesPage;

        if (startDate != null && endDate != null) {
            expensesPage = expenseRepository.findByUserIdAndDateBetween(SecurityUtils.getCurrentUserId(), startDate, endDate, pageable);
        } else if (startDate != null) {
            expensesPage = expenseRepository.findByUserIdAndDateGreaterThanEqual(SecurityUtils.getCurrentUserId(), startDate, pageable);
        } else if (endDate != null) {
            expensesPage = expenseRepository.findByUserIdAndDateLessThanEqual(SecurityUtils.getCurrentUserId(), endDate, pageable);
        } else {
            expensesPage = expenseRepository.findAllByUserId(SecurityUtils.getCurrentUserId(), pageable);
        }

        return expenseMapper.toPageResponseDTO(expensesPage);
    }

    public ExpenseResponseDTO findExpense(UUID id) {
        Expense expense = expenseRepository.findByIdAndUserId(id, SecurityUtils.getCurrentUserId())
                                           .orElseThrow(
                                                    () -> new ResourceNotFoundException("Expense with id '%s' not found".formatted(id))
                                           );

        return expenseMapper.toDTO(expense);
    }

    public void deleteExpense(UUID id) {
        Expense expense = expenseRepository.findByIdAndUserId(id, SecurityUtils.getCurrentUserId())
                                           .orElseThrow(
                                                    () -> new ResourceNotFoundException("Expense with id '%s' not found".formatted(id))
                                           );

        expenseRepository.delete(expense);
    }

    @Transactional
    public ExpenseResponseDTO updateExpense(UUID id, UpdateExpenseRequestDTO dto) {
        Expense expense = expenseRepository.findByIdAndUserId(id, SecurityUtils.getCurrentUserId())
                                           .orElseThrow(() -> new ResourceNotFoundException("Expense with id '%s' not found".formatted(id)));

        updateFields(dto, expense);
        expenseRepository.save(expense);

        return expenseMapper.toDTO(expense);
    }

    private void updateFields(UpdateExpenseRequestDTO dto, Expense expense) {
        if (dto.name() != null) expense.setName(dto.name());
        if (dto.description() != null) expense.setDescription(dto.description());
        if (dto.amount() != null) expense.setAmount(dto.amount());
        if (dto.category() != null) expense.setCategory(Category.fromString(dto.category()));
        if (dto.date() != null) expense.setDate(dto.date());

        expense.setUpdatedAt(Instant.now());
    }

}
