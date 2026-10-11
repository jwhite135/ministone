package com.minishiftsync.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;
import com.minishiftsync.store.DataStore;
import com.minishiftsync.web.dto.EmployeeRequest;
import com.minishiftsync.web.dto.ShiftRequest;

@Service
public class SchedulingService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US);

	private final DataStore store;

	public SchedulingService(DataStore store) {
		this.store = store;
	}

	public List<Employee> listEmployees() {
		return store.findAllEmployees();
	}

	public Employee getEmployee(long id) {
		return store.findEmployee(id)
				.orElseThrow(() -> ApiException.notFound("Employee " + id + " does not exist."));
	}

	public Employee createEmployee(EmployeeRequest request) {
		return store.addEmployee(request.name().trim(), request.position().trim(), request.hourlyRate());
	}

	public List<Shift> listShifts(Long employeeId) {
		return store.findAllShifts().stream()
				.filter(s -> employeeId == null || s.employeeId() == employeeId)
				.sorted(ScheduleRules.BY_START)
				.toList();
	}

	// Shift writes are synchronized so the overlap check and the save can't interleave.
	public synchronized Shift createShift(ShiftRequest request) {
		validate(0, request);
		return store.addShift(request.employeeId(), request.date(), request.startTime(), request.endTime());
	}

	public synchronized Shift updateShift(long id, ShiftRequest request) {
		store.findShift(id).orElseThrow(() -> ApiException.notFound("Shift " + id + " does not exist."));
		validate(id, request);
		return store.updateShift(id, request.employeeId(), request.date(), request.startTime(), request.endTime())
				.orElseThrow();
	}

	public synchronized void deleteShift(long id) {
		if (!store.deleteShift(id)) {
			throw ApiException.notFound("Shift " + id + " does not exist.");
		}
	}

	public WeekSchedule getWeek(LocalDate anyDayInWeek, String position) {
		List<Employee> employees = store.findAllEmployees().stream()
				.filter(e -> position == null || position.isBlank() || e.position().equalsIgnoreCase(position.trim()))
				.toList();
		return ScheduleRules.summarizeWeek(anyDayInWeek, employees, store.findAllShifts());
	}

	/** {@code shiftId} is 0 for a new shift; otherwise that shift is excluded from the overlap check. */
	private void validate(long shiftId, ShiftRequest request) {
		Employee employee = store.findEmployee(request.employeeId())
				.orElseThrow(() -> ApiException.badRequest("Employee " + request.employeeId() + " does not exist."));
		if (request.startTime().equals(request.endTime())) {
			throw ApiException.badRequest("A shift's start and end time cannot be the same.");
		}

		Shift candidate = new Shift(shiftId, employee.id(), request.date(), request.startTime(), request.endTime());
		ScheduleRules.findOverlap(candidate, store.findAllShifts()).ifPresent(existing -> {
			throw ApiException.conflict(employee.name() + " already has a shift on " + describe(existing)
					+ " that overlaps this one.");
		});
	}

	private static String describe(Shift shift) {
		return shift.date().format(DAY) + " from " + shift.startTime() + " to " + shift.endTime()
				+ (shift.endsNextDay() ? " (next day)" : "");
	}

}
