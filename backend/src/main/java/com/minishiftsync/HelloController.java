package com.minishiftsync;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	// A record is an immutable data class; Jackson turns it into JSON automatically.
	public record Greeting(String message, LocalDateTime serverTime) {
	}

	@GetMapping("/api/hello")
	public Greeting hello(@RequestParam(defaultValue = "world") String name) {
		return new Greeting("Hello, " + name + "! Mini ShiftSync backend is running.", LocalDateTime.now());
	}

}
