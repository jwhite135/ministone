package com.minishiftsync.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** What the client sends to create an employee. The server assigns the id. */
public record EmployeeRequest(
		@NotBlank(message = "is required") @Size(max = 50) String name,
		@NotBlank(message = "is required") @Size(max = 50) String position,
		@NotNull(message = "is required") @Positive
		@Digits(integer = 4, fraction = 2, message = "must be a dollar amount like 15.50 (max 9999.99)")
		BigDecimal hourlyRate) {
}
