package com.minishiftsync.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

	// These methods aren't named getX/isX, so Jackson doesn't write them to JSON.

	public boolean endsNextDay() {
		return !endTime.isAfter(startTime);
	}

	public LocalDateTime startDateTime() {
		return date.atTime(startTime);
	}

	public LocalDateTime endDateTime() {
		return (endsNextDay() ? date.plusDays(1) : date).atTime(endTime);
	}

}
