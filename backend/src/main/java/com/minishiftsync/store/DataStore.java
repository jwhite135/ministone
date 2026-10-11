package com.minishiftsync.store;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minishiftsync.model.Employee;
import com.minishiftsync.model.Shift;

/**
 * Holds all employees and shifts in memory. Spring creates exactly one instance (a singleton),
 * but web requests arrive on many threads at once, so every method is synchronized.
 */
@Component
public class DataStore {

	private final List<Employee> employees = new ArrayList<>();
	private final List<Shift> shifts = new ArrayList<>();
	private long nextEmployeeId = 1;
	private long nextShiftId = 1;

	public synchronized List<Employee> findAllEmployees() {
		return List.copyOf(employees);
	}

	public synchronized Optional<Employee> findEmployee(long id) {
		return employees.stream().filter(e -> e.id() == id).findFirst();
	}

	public synchronized Employee addEmployee(String name, String position, BigDecimal hourlyRate) {
		Employee employee = new Employee(nextEmployeeId++, name, position, hourlyRate);
		employees.add(employee);
		return employee;
	}

	public synchronized List<Shift> findAllShifts() {
		return List.copyOf(shifts);
	}

	public synchronized Shift addShift(long employeeId, LocalDate date, LocalTime startTime, LocalTime endTime) {
		Shift shift = new Shift(nextShiftId++, employeeId, date, startTime, endTime);
		shifts.add(shift);
		return shift;
	}

}
