package com.minishiftsync.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;
import com.minishiftsync.service.WeekSchedule.EmployeeTotals;
import com.minishiftsync.service.WeekSchedule.ShiftEntry;

/**
 * Pure scheduling rules: no Spring, no files, no HTTP. Everything here is a function of its
 * inputs, which makes it easy to unit test.
 */
public final class ScheduleRules {

	public static final Comparator<Shift> BY_START = Comparator.comparing(Shift::startDateTime);

	private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

	private ScheduleRules() {
	}

	/**
	 * Two shifts overlap if each starts before the other ends. Shifts are half-open intervals
	 * [start, end), so one ending at 13:00 and another starting at 13:00 do not overlap.
	 */
	public static boolean overlaps(Shift a, Shift b) {
		return a.startDateTime().isBefore(b.endDateTime()) && b.startDateTime().isBefore(a.endDateTime());
	}

	/** Finds the earliest of the employee's other shifts that overlaps {@code candidate}, if any. */
	public static Optional<Shift> findOverlap(Shift candidate, Collection<Shift> existing) {
		return existing.stream()
				.filter(s -> s.employeeId() == candidate.employeeId())
				.filter(s -> s.id() != candidate.id())
				.filter(s -> overlaps(s, candidate))
				.min(BY_START);
	}

	/** Minutes of {@code shift} that fall inside [from, to). Zero if they don't intersect. */
	public static long minutesWithin(Shift shift, LocalDateTime from, LocalDateTime to) {
		LocalDateTime start = shift.startDateTime().isAfter(from) ? shift.startDateTime() : from;
		LocalDateTime end = shift.endDateTime().isBefore(to) ? shift.endDateTime() : to;
		return start.isBefore(end) ? Duration.between(start, end).toMinutes() : 0;
	}

	public static BigDecimal hours(long minutes) {
		return BigDecimal.valueOf(minutes).divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
	}

	/** Pay for {@code minutes} of work, rounded to the cent once at the end. */
	public static BigDecimal pay(long minutes, BigDecimal hourlyRate) {
		return hourlyRate.multiply(BigDecimal.valueOf(minutes)).divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
	}

	public static LocalDate weekStart(LocalDate anyDay) {
		return anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	/**
	 * Builds the Monday-to-Sunday week containing {@code anyDay}. Hours count in the week they are
	 * worked, so an overnight shift from Sunday into Monday is split between the two weeks.
	 */
	public static WeekSchedule summarizeWeek(LocalDate anyDay, List<Employee> employees, List<Shift> shifts) {
		LocalDate monday = weekStart(anyDay);
		LocalDateTime from = monday.atStartOfDay();
		LocalDateTime to = monday.plusWeeks(1).atStartOfDay();
		Map<Long, Employee> employeesById = employees.stream()
				.collect(Collectors.toMap(Employee::id, Function.identity()));

		List<Shift> weekShifts = shifts.stream()
				.filter(s -> minutesWithin(s, from, to) > 0)
				.filter(s -> employeesById.containsKey(s.employeeId()))
				.sorted(BY_START)
				.toList();

		List<ShiftEntry> entries = weekShifts.stream()
				.map(s -> {
					Employee e = employeesById.get(s.employeeId());
					long total = Duration.between(s.startDateTime(), s.endDateTime()).toMinutes();
					return new ShiftEntry(s.id(), s.employeeId(), e.name(), e.position(), s.date(), s.startTime(),
							s.endTime(), s.endsNextDay(), hours(total), hours(minutesWithin(s, from, to)));
				})
				.toList();

		List<EmployeeTotals> totals = employees.stream()
				.map(e -> {
					List<Shift> own = weekShifts.stream().filter(s -> s.employeeId() == e.id()).toList();
					long minutes = own.stream().mapToLong(s -> minutesWithin(s, from, to)).sum();
					return new EmployeeTotals(e.id(), e.name(), e.position(), e.hourlyRate(), own.size(),
							hours(minutes), pay(minutes, e.hourlyRate()));
				})
				.toList();

		long totalMinutes = weekShifts.stream().mapToLong(s -> minutesWithin(s, from, to)).sum();
		BigDecimal totalPay = totals.stream().map(EmployeeTotals::estimatedPay).reduce(BigDecimal.ZERO, BigDecimal::add);

		return new WeekSchedule(monday, monday.plusDays(6), entries, totals, hours(totalMinutes), totalPay);
	}

}
