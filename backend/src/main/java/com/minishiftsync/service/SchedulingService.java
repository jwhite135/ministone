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

/** Business rules for employees and shifts. Controllers call this; this calls the DataStore. */
@Service
public class SchedulingService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US);

	private final DataStore store;

	// Spring sees this constructor and passes in the DataStore bean ("dependency injection").
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

	/**
	 * Synchronized (like the other shift writes) so the overlap check and the save happen as one
	 * step. Otherwise two requests arriving together could both pass the check and both be saved
	 * ("check-then-act" race).
	 */
	public synchronized Shift createShift(ShiftRequest request) {
		validate(0, request);
		return store.addShift(request.employeeId(), request.date(), request.startTime(), request.endTime());
	}

	public synchronized Shift updateShift(long id, ShiftRequest request) {
		if (store.findAllShifts().stream().noneMatch(s -> s.id() == id)) {
			throw ApiException.notFound("Shift " + id + " does not exist.");
		}
		// Passing the real id means the overlap check ignores this shift's own current time slot.
		validate(id, request);
		return store.updateShift(id, request.employeeId(), request.date(), request.startTime(), request.endTime())
				.orElseThrow(() -> ApiException.notFound("Shift " + id + " does not exist."));
	}

	public synchronized void deleteShift(long id) {
		if (!store.deleteShift(id)) {
			throw ApiException.notFound("Shift " + id + " does not exist.");
		}
	}

	/** The week containing {@code anyDayInWeek}, optionally limited to one position (case-insensitive). */
	public WeekSchedule getWeek(LocalDate anyDayInWeek, String position) {
		List<Employee> employees = store.findAllEmployees().stream()
				.filter(e -> position == null || position.isBlank() || e.position().equalsIgnoreCase(position.trim()))
				.toList();
		return ScheduleRules.summarizeWeek(anyDayInWeek, employees, store.findAllShifts());
	}

	/** Checks a new or edited shift. {@code shiftId} is 0 for a new shift. */
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
