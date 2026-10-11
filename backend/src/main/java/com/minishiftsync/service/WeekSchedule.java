package com.minishiftsync.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Everything the weekly view needs: the week's shifts and each employee's hours and pay. */
public record WeekSchedule(
		LocalDate weekStart,
		LocalDate weekEnd,
		List<ShiftEntry> shifts,
		List<EmployeeTotals> employees,
		BigDecimal totalHours,
		BigDecimal totalPay) {

	/**
	 * A shift with its employee's name attached. {@code hoursInWeek} can be less than
	 * {@code hours} when an overnight shift crosses the start or end of the week.
	 */
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
