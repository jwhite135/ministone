package com.minishiftsync.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;
import com.minishiftsync.service.WeekSchedule.EmployeeTotals;

class ScheduleRulesTest {

	private static final LocalDate MON = LocalDate.of(2026, 10, 12);
	private static final LocalDate TUE = MON.plusDays(1);
	private static final LocalDate SUN = MON.plusDays(6);

	private static Shift shift(long id, long employeeId, LocalDate date, String start, String end) {
		return new Shift(id, employeeId, date, LocalTime.parse(start), LocalTime.parse(end));
	}

	@Nested
	class Overlap {

		private final Shift morning = shift(1, 1, MON, "09:00", "13:00");

		@Test
		void partialOverlapIsRejected() {
			assertThat(ScheduleRules.overlaps(morning, shift(0, 1, MON, "12:00", "15:00"))).isTrue();
		}

		@Test
		void shiftInsideAnotherIsRejected() {
			assertThat(ScheduleRules.overlaps(morning, shift(0, 1, MON, "10:00", "11:00"))).isTrue();
		}

		@Test
		void shiftCoveringAnotherIsRejected() {
			assertThat(ScheduleRules.overlaps(morning, shift(0, 1, MON, "08:00", "14:00"))).isTrue();
		}

		@Test
		void backToBackShiftsAreAllowed() {
			assertThat(ScheduleRules.overlaps(morning, shift(0, 1, MON, "13:00", "17:00"))).isFalse();
			assertThat(ScheduleRules.overlaps(morning, shift(0, 1, MON, "05:00", "09:00"))).isFalse();
		}

		@Test
		void splitShiftOnSameDayIsAllowed() {
			assertThat(ScheduleRules.findOverlap(shift(0, 1, MON, "17:00", "21:00"), List.of(morning))).isEmpty();
		}

		@Test
		void overnightShiftConflictsWithNextMorning() {
			Shift overnight = shift(1, 1, MON, "22:00", "06:00");
			assertThat(ScheduleRules.overlaps(overnight, shift(0, 1, TUE, "05:00", "09:00"))).isTrue();
			assertThat(ScheduleRules.overlaps(overnight, shift(0, 1, TUE, "06:00", "09:00"))).isFalse();
		}

		@Test
		void newOvernightShiftConflictsWithLateShiftThatDay() {
			Shift late = shift(1, 1, MON, "20:00", "23:30");
			assertThat(ScheduleRules.overlaps(late, shift(0, 1, MON, "23:00", "03:00"))).isTrue();
		}

		@Test
		void otherEmployeesShiftsAreIgnored() {
			assertThat(ScheduleRules.findOverlap(shift(0, 2, MON, "09:00", "13:00"), List.of(morning))).isEmpty();
		}

		@Test
		void reportsTheEarliestConflictingShift() {
			Shift lunch = shift(2, 1, MON, "13:00", "15:00");
			Shift candidate = shift(0, 1, MON, "12:00", "14:00");
			assertThat(ScheduleRules.findOverlap(candidate, List.of(lunch, morning))).contains(morning);
		}

	}

	@Nested
	class WeeklySummary {

		private final Employee ava = new Employee(1, "Ava", "Server", new BigDecimal("15.50"));
		private final Employee dev = new Employee(2, "Dev", "Cook", new BigDecimal("19.75"));
		private final Employee idle = new Employee(3, "Idle", "Host", new BigDecimal("14.00"));

		private EmployeeTotals totalsFor(WeekSchedule week, long employeeId) {
			return week.employees().stream().filter(t -> t.employeeId() == employeeId).findFirst().orElseThrow();
		}

		@Test
		void addsUpSplitShiftsAndComputesPay() {
			List<Shift> shifts = List.of(
					shift(1, 1, MON, "09:00", "13:00"),
					shift(2, 1, MON, "17:00", "20:30"));

			WeekSchedule week = ScheduleRules.summarizeWeek(MON, List.of(ava), shifts);

			EmployeeTotals totals = totalsFor(week, 1);
			assertThat(totals.shiftCount()).isEqualTo(2);
			assertThat(totals.hours()).isEqualByComparingTo("7.5");
			assertThat(totals.estimatedPay()).isEqualByComparingTo("116.25"); // 7.5 x 15.50
		}

		@Test
		void anyDayMapsToItsMondayToSundayWeek() {
			WeekSchedule week = ScheduleRules.summarizeWeek(LocalDate.of(2026, 10, 15), List.of(ava), List.of());

			assertThat(week.weekStart()).isEqualTo(MON);
			assertThat(week.weekEnd()).isEqualTo(SUN);
		}

		@Test
		void sundayOvernightShiftIsSplitAcrossWeeks() {
			Shift sundayNight = shift(1, 2, SUN, "22:00", "02:00");

			WeekSchedule thisWeek = ScheduleRules.summarizeWeek(MON, List.of(dev), List.of(sundayNight));
			WeekSchedule nextWeek = ScheduleRules.summarizeWeek(MON.plusWeeks(1), List.of(dev), List.of(sundayNight));

			assertThat(totalsFor(thisWeek, 2).hours()).isEqualByComparingTo("2");
			assertThat(totalsFor(nextWeek, 2).hours()).isEqualByComparingTo("2");
			assertThat(thisWeek.shifts().getFirst().hours()).isEqualByComparingTo("4");
			assertThat(thisWeek.shifts().getFirst().hoursInWeek()).isEqualByComparingTo("2");
		}

		@Test
		void shiftsFromOtherWeeksAreExcluded() {
			List<Shift> shifts = List.of(
					shift(1, 1, MON.minusDays(3), "09:00", "17:00"),
					shift(2, 1, MON.plusWeeks(1), "09:00", "17:00"));

			WeekSchedule week = ScheduleRules.summarizeWeek(MON, List.of(ava), shifts);

			assertThat(week.shifts()).isEmpty();
			assertThat(totalsFor(week, 1).hours()).isEqualByComparingTo("0");
		}

		@Test
		void employeesWithNoShiftsStillAppearWithZero() {
			WeekSchedule week = ScheduleRules.summarizeWeek(MON, List.of(ava, idle),
					List.of(shift(1, 1, MON, "09:00", "17:00")));

			EmployeeTotals totals = totalsFor(week, 3);
			assertThat(totals.shiftCount()).isZero();
			assertThat(totals.estimatedPay()).isEqualByComparingTo("0");
		}

		@Test
		void payIsRoundedToTheCentOnceFromTotalMinutes() {
			// Rounding each 20-minute shift first would give 3 x $3.34 = $10.02.
			Employee e = new Employee(1, "Pat", "Host", new BigDecimal("10.01"));
			List<Shift> shifts = List.of(
					shift(1, 1, MON, "09:00", "09:20"),
					shift(2, 1, MON, "10:00", "10:20"),
					shift(3, 1, MON, "11:00", "11:20"));

			WeekSchedule week = ScheduleRules.summarizeWeek(MON, List.of(e), shifts);

			assertThat(totalsFor(week, 1).estimatedPay()).isEqualByComparingTo("10.01");
		}

		@Test
		void weekTotalsAddUpAcrossEmployees() {
			List<Shift> shifts = List.of(
					shift(1, 1, MON, "09:00", "17:00"), // 8h x 15.50 = 124.00
					shift(2, 2, TUE, "22:00", "06:00")); // 8h x 19.75 = 158.00

			WeekSchedule week = ScheduleRules.summarizeWeek(MON, List.of(ava, dev), shifts);

			assertThat(week.totalHours()).isEqualByComparingTo("16");
			assertThat(week.totalPay()).isEqualByComparingTo("282.00");
		}

	}

}
