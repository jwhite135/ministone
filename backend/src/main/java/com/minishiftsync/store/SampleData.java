package com.minishiftsync.store;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;

public final class SampleData {

	private SampleData() {
	}

	public static StoreData create(LocalDate today) {
		// Seed next week so demo shifts are added in the future, not the past.
		LocalDate monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));

		List<Employee> employees = List.of(
				new Employee(1, "Ava Lopez", "Server", new BigDecimal("15.50")),
				new Employee(2, "Ben Ortiz", "Cook", new BigDecimal("18.00")),
				new Employee(3, "Chloe Nguyen", "Host", new BigDecimal("14.25")),
				new Employee(4, "Dev Patel", "Cook", new BigDecimal("19.75")));

		// Includes a split shift (Ava, Monday) and a Sunday overnight shift that runs into next week.
		List<Shift> shifts = new ArrayList<>();
		addShift(shifts, 1, monday, "09:00", "13:00");
		addShift(shifts, 1, monday, "17:00", "21:00");
		addShift(shifts, 1, monday.plusDays(2), "11:00", "19:00");
		addShift(shifts, 1, monday.plusDays(4), "16:00", "23:00");
		addShift(shifts, 2, monday, "06:00", "14:00");
		addShift(shifts, 2, monday.plusDays(1), "06:00", "14:00");
		addShift(shifts, 2, monday.plusDays(3), "22:00", "06:00");
		addShift(shifts, 2, monday.plusDays(5), "10:00", "18:00");
		addShift(shifts, 3, monday.plusDays(1), "11:00", "15:00");
		addShift(shifts, 3, monday.plusDays(4), "17:00", "22:00");
		addShift(shifts, 3, monday.plusDays(5), "17:00", "22:00");
		addShift(shifts, 4, monday.plusDays(2), "14:00", "22:00");
		addShift(shifts, 4, monday.plusDays(4), "22:00", "04:00");
		addShift(shifts, 4, monday.plusDays(6), "22:00", "02:00");

		return new StoreData(employees, shifts);
	}

	private static void addShift(List<Shift> shifts, long employeeId, LocalDate date, String start, String end) {
		shifts.add(new Shift(shifts.size() + 1, employeeId, date, LocalTime.parse(start), LocalTime.parse(end)));
	}

}
