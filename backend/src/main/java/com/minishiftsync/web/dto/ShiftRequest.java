package com.minishiftsync.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/** What the client sends to create a shift. Times are "HH:mm"; dates are "yyyy-MM-dd". */
public record ShiftRequest(
		@NotNull Long employeeId,
		@NotNull LocalDate date,
		@NotNull LocalTime startTime,
		@NotNull LocalTime endTime) {
}
