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

}
