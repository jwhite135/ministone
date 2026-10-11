package com.minishiftsync.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record ShiftRequest(
		@NotNull(message = "is required") Long employeeId,
		@NotNull(message = "is required") LocalDate date,
		@NotNull(message = "is required") LocalTime startTime,
		@NotNull(message = "is required") LocalTime endTime) {
}
