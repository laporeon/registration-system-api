package com.laporeon.expensetracker.mappers;

import com.laporeon.expensetracker.dtos.request.CreateExpenseRequestDTO;
import com.laporeon.expensetracker.dtos.response.ExpenseResponseDTO;
import com.laporeon.expensetracker.dtos.response.PageResponseDTO;
import com.laporeon.expensetracker.entities.Expense;
import com.laporeon.expensetracker.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ExpenseMapper {

    public Expense toEntity(CreateExpenseRequestDTO dto, UUID userId) {
        return Expense.builder()
                      .name(dto.name())
                      .description(dto.description())
                      .amount(dto.amount())
                      .category(Category.fromString(dto.category()))
                      .userId(userId)
                      .date(dto.date())
                      .createdAt(Instant.now())
                      .updatedAt(Instant.now())
                      .build();
    }

    public ExpenseResponseDTO toDTO(Expense expense) {
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
    }

    public PageResponseDTO<ExpenseResponseDTO> toPageResponseDTO(Page<Expense> page) {
        return new PageResponseDTO<>(
                page.getContent().stream().map(this::toDTO).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty(),
                page.getSort().isSorted(),
                page.getSort().isUnsorted()
        );
    }

}
