package com.bgsistemas.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// BGSI-326: lets @Scheduled methods (the nightly cleanups) run by themselves
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
