package com.minishiftsync.web;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minishiftsync.service.SchedulingService;
import com.minishiftsync.service.WeekSchedule;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

	private final SchedulingService service;

	public ScheduleController(SchedulingService service) {
		this.service = service;
	}

	/** The Monday-to-Sunday week containing {@code date} (default: today), optionally for one position. */
	@GetMapping("/week")
	public WeekSchedule week(@RequestParam(required = false) LocalDate date,
			@RequestParam(required = false) String position) {
		return service.getWeek(date != null ? date : LocalDate.now(), position);
	}

}
