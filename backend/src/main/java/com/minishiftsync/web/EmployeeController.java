package com.minishiftsync.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minishiftsync.model.Employee;
import com.minishiftsync.service.SchedulingService;
import com.minishiftsync.web.dto.EmployeeRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	private final SchedulingService service;

	public EmployeeController(SchedulingService service) {
		this.service = service;
	}

	@GetMapping
	public List<Employee> list() {
		return service.listEmployees();
	}

	@GetMapping("/{id}")
	public Employee get(@PathVariable long id) {
		return service.getEmployee(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Employee create(@Valid @RequestBody EmployeeRequest request) {
		return service.createEmployee(request);
	}

}
