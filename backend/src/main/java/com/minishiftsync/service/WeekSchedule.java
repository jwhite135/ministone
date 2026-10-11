package com.minishiftsync.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record WeekSchedule(
		LocalDate weekStart,
		LocalDate weekEnd,
		List<ShiftEntry> shifts,
		List<EmployeeTotals> employees,
		BigDecimal totalHours,
		BigDecimal totalPay) {

	/** {@code hoursInWeek} is less than {@code hours} when an overnight shift crosses a week boundary. */
	public record ShiftEntry(
			long id,
			long employeeId,
			String employeeName,
			String position,
			LocalDate date,
			@JsonFormat(pattern = "HH:mm") LocalTime startTime,
			@JsonFormat(pattern = "HH:mm") LocalTime endTime,
			boolean endsNextDay,
			BigDecimal hours,
			BigDecimal hoursInWeek) {
	}

	public record EmployeeTotals(
			long employeeId,
			String name,
			String position,
			BigDecimal hourlyRate,
			int shiftCount,
			BigDecimal hours,
			BigDecimal estimatedPay) {
	}

}
