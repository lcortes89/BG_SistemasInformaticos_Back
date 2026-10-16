package com.bgsistemas.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// BGSI-120: lets @Async methods (like sending an email) run in the background
@Configuration
@EnableAsync
public class AsyncConfig {
}
