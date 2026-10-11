package com.minishiftsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import com.minishiftsync.model.Shift;
import com.minishiftsync.store.DataStore;
import com.minishiftsync.web.dto.ShiftRequest;

import tools.jackson.databind.json.JsonMapper;

/** Exercises the service against the real DataStore, seeded with the sample data. */
class SchedulingServiceTest {

	private static final LocalDate MON = LocalDate.of(2026, 10, 12);

	@TempDir
	Path tempDir;

	private SchedulingService service;

	@BeforeEach
	void setUp() {
		DataStore store = new DataStore(JsonMapper.builder().build(), tempDir.resolve("data.json"), MON);
		service = new SchedulingService(store);
	}

	private static ShiftRequest request(long employeeId, LocalDate date, String start, String end) {
		return new ShiftRequest(employeeId, date, LocalTime.parse(start), LocalTime.parse(end));
	}

	@Test
	void sampleDataHasNoOverlaps() {
		var shifts = service.listShifts(null);

		assertThat(shifts).allSatisfy(s -> assertThat(ScheduleRules.findOverlap(s, shifts)).isEmpty());
	}

	@Test
	void rejectsOverlappingShiftWithReadableReason() {
		// Sample data: Ava (id 1) works Monday 09:00-13:00.
		assertThatThrownBy(() -> service.createShift(request(1, MON, "12:00", "14:00")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT))
				.hasMessage("Ava Lopez already has a shift on Mon, Oct 12 from 09:00 to 13:00 that overlaps this one.");
	}

	@Test
	void rejectsShiftOverlappingPreviousNightsOvernightShift() {
		// Sample data: Ben (id 2) works Thursday 22:00 to Friday 06:00.
		assertThatThrownBy(() -> service.createShift(request(2, MON.plusDays(4), "05:00", "09:00")))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("Thu, Oct 15 from 22:00 to 06:00 (next day)");
	}

	@Test
	void allowsAnotherShiftInAGapOnTheSameDay() {
		// Ava has 09:00-13:00 and 17:00-21:00 on Monday; 13:00-17:00 fits exactly between.
		Shift created = service.createShift(request(1, MON, "13:00", "17:00"));

		assertThat(created.id()).isPositive();
		assertThat(service.listShifts(1L)).contains(created);
	}

	@Test
	void sameTimeForDifferentEmployeeIsFine() {
		assertThat(service.createShift(request(3, MON, "09:00", "13:00"))).isNotNull();
	}

	@Test
	void rejectedShiftIsNotSaved() {
		int before = service.listShifts(null).size();

		assertThatThrownBy(() -> service.createShift(request(1, MON, "08:00", "10:00")));

		assertThat(service.listShifts(null)).hasSize(before);
	}

	// ---------- Editing and deleting ----------

	/** Ava's Monday 09:00-13:00 shift from the sample data. */
	private Shift avasMondayMorning() {
		return service.listShifts(1L).stream()
				.filter(s -> s.date().equals(MON) && s.startTime().equals(LocalTime.of(9, 0)))
				.findFirst().orElseThrow();
	}

	private static void assertStatus(Runnable action, HttpStatus status) {
		assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
				ex -> assertThat(ex.getStatus()).isEqualTo(status));
	}

	@Test
	void editingAShiftDoesNotConflictWithItsOwnOldTimes() {
		Shift original = avasMondayMorning();

		// 08:00-12:00 overlaps the old 09:00-13:00, but that's the same shift, so it's allowed.
		Shift updated = service.updateShift(original.id(), request(1, MON, "08:00", "12:00"));

		assertThat(updated.id()).isEqualTo(original.id());
		assertThat(updated.startTime()).isEqualTo(LocalTime.of(8, 0));
		assertThat(service.listShifts(null)).doesNotContain(original).contains(updated);
	}

	@Test
	void editingIntoAnotherShiftIsRejected() {
		Shift original = avasMondayMorning();

		// Ava's other Monday shift is 17:00-21:00.
		assertStatus(() -> service.updateShift(original.id(), request(1, MON, "16:00", "18:00")), HttpStatus.CONFLICT);
		assertThat(service.listShifts(null)).contains(original);
	}

	@Test
	void shiftCanBeReassignedToAnotherEmployee() {
		Shift original = avasMondayMorning();

		// Chloe (id 3) is free Monday morning.
		Shift updated = service.updateShift(original.id(), request(3, MON, "09:00", "13:00"));

		assertThat(updated.employeeId()).isEqualTo(3);
	}

	@Test
	void editingOrDeletingAMissingShiftIs404() {
		assertStatus(() -> service.updateShift(999, request(1, MON, "09:00", "10:00")), HttpStatus.NOT_FOUND);
		assertStatus(() -> service.deleteShift(999), HttpStatus.NOT_FOUND);
	}

	@Test
	void deletedShiftIsGoneAndFreesTheTimeSlot() {
		Shift original = avasMondayMorning();

		service.deleteShift(original.id());

		assertThat(service.listShifts(null)).doesNotContain(original);
		assertThat(service.createShift(request(1, MON, "10:00", "12:00"))).isNotNull();
	}

	// ---------- Position filter ----------

	@Test
	void weekCanBeFilteredByPosition() {
		WeekSchedule cooks = service.getWeek(MON, "cook"); // case-insensitive

		assertThat(cooks.employees()).extracting(WeekSchedule.EmployeeTotals::name)
				.containsExactly("Ben Ortiz", "Dev Patel");
		assertThat(cooks.shifts()).allSatisfy(s -> assertThat(s.position()).isEqualTo("Cook"));
		assertThat(cooks.totalHours()).isEqualByComparingTo("48"); // Ben 32 + Dev 16
	}

	@Test
	void blankPositionMeansEveryone() {
		assertThat(service.getWeek(MON, "").employees()).hasSize(4);
		assertThat(service.getWeek(MON, null).employees()).hasSize(4);
	}

}
