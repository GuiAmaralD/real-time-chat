package com.guiamaral.real_time_chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootApplication
public class RealTimeChatApplication {

	private static final Logger log = LoggerFactory.getLogger(RealTimeChatApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(RealTimeChatApplication.class, args);
	}

	@Bean
	@ConditionalOnProperty(name = "app.redis.flush-on-startup", havingValue = "true")
	CommandLineRunner flushRedisOnStartup(StringRedisTemplate redisTemplate) {
		return args -> {
			RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
			if (connectionFactory == null) {
				throw new IllegalStateException("RedisConnectionFactory was not initialized.");
			}

			try (RedisConnection connection = connectionFactory.getConnection()) {
				connection.serverCommands().flushDb();
			}

			log.warn("Redis was flushed on startup (flushdb) because app.redis.flush-on-startup=true.");
		};
	}

	@Bean
	@ConditionalOnProperty(name = "app.redis.verify-on-startup", havingValue = "true")
	CommandLineRunner verifyRedisConnection(StringRedisTemplate redisTemplate) {
		return args -> {
			RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
			if (connectionFactory == null) {
				throw new IllegalStateException("RedisConnectionFactory was not initialized.");
			}

			String response;
			try (RedisConnection connection = connectionFactory.getConnection()) {
				response = connection.ping();
			}

			if (response == null || !"PONG".equalsIgnoreCase(response)) {
				throw new IllegalStateException("Redis did not respond to ping during startup.");
			}
			log.info("Redis connection established successfully: {}", response);
		};
	}

}
