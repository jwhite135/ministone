package com.minishiftsync.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;
import com.minishiftsync.store.DataStore;
import com.minishiftsync.web.dto.EmployeeRequest;
import com.minishiftsync.web.dto.ShiftRequest;

/** Business rules for employees and shifts. Controllers call this; this calls the DataStore. */
@Service
public class SchedulingService {

	private static final Comparator<Shift> BY_START = Comparator.comparing(Shift::date)
			.thenComparing(Shift::startTime);

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
				.sorted(BY_START)
				.toList();
	}

	public Shift createShift(ShiftRequest request) {
		if (store.findEmployee(request.employeeId()).isEmpty()) {
			throw ApiException.badRequest("Employee " + request.employeeId() + " does not exist.");
		}
		if (request.startTime().equals(request.endTime())) {
			throw ApiException.badRequest("A shift's start and end time cannot be the same.");
		}
		return store.addShift(request.employeeId(), request.date(), request.startTime(), request.endTime());
	}

}
