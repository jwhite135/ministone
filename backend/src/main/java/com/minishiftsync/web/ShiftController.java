package com.minishiftsync.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minishiftsync.model.Shift;
import com.minishiftsync.service.SchedulingService;
import com.minishiftsync.web.dto.ShiftRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

	private final SchedulingService service;

	public ShiftController(SchedulingService service) {
		this.service = service;
	}

	@GetMapping
	public List<Shift> list(@RequestParam(required = false) Long employeeId) {
		return service.listShifts(employeeId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Shift create(@Valid @RequestBody ShiftRequest request) {
		return service.createShift(request);
	}

}
