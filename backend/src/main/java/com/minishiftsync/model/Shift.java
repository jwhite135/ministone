package com.minishiftsync.model;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A shift that starts on {@code date} at {@code startTime}. If {@code endTime} is earlier than
 * {@code startTime}, the shift ends on the following day (e.g. 22:00 to 06:00).
 */
public record Shift(
		long id,
		long employeeId,
		LocalDate date,
		@JsonFormat(pattern = "HH:mm") LocalTime startTime,
		@JsonFormat(pattern = "HH:mm") LocalTime endTime) {
}
