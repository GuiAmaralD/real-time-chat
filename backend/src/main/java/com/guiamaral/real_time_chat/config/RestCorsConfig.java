package com.guiamaral.real_time_chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RestCorsConfig implements WebMvcConfigurer {

	private final String[] allowedOriginPatterns;

	public RestCorsConfig(
			@Value("${app.cors.allowed-origin-patterns:*}") String allowedOriginPatterns
	) {
		this.allowedOriginPatterns = allowedOriginPatterns.split("\\s*,\\s*");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOriginPatterns(allowedOriginPatterns)
				.allowedMethods("*")
				.allowedHeaders("*")
				.maxAge(3600);
	}
}
