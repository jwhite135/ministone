package com.minishiftsync.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/** What the client sends to create a shift. Times are "HH:mm"; dates are "yyyy-MM-dd". */
public record ShiftRequest(
		@NotNull(message = "is required") Long employeeId,
		@NotNull(message = "is required") LocalDate date,
		@NotNull(message = "is required") LocalTime startTime,
		@NotNull(message = "is required") LocalTime endTime) {
}
