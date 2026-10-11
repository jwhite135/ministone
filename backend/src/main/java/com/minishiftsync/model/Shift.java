package com.minishiftsync.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** A shift whose end time is not after its start time ends the next day. */
public record Shift(
		long id,
		long employeeId,
		LocalDate date,
		@JsonFormat(pattern = "HH:mm") LocalTime startTime,
		@JsonFormat(pattern = "HH:mm") LocalTime endTime) {

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
