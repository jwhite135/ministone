package com.minishiftsync.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the React dev server (a different origin: another port) call this API from the browser.
 * Without this, the browser blocks the frontend from reading our responses.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	public CorsConfig(@Value("${minishiftsync.cors.allowed-origins}") String[] allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "DELETE");
	}

}
