package com.bgsistemas.api.config;

import com.bgsistemas.api.security.ConfirmationInterceptor;
import com.bgsistemas.api.security.ConfirmationTokenService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// BGSI-275: registers the check of @RequiresConfirmation.
// Optional so the @WebMvcTest slices (without ConfirmationTokenService) still start
@Configuration
public class ConfirmationConfig implements WebMvcConfigurer {

    private final ObjectProvider<ConfirmationTokenService> confirmationTokens;

    public ConfirmationConfig(ObjectProvider<ConfirmationTokenService> confirmationTokens) {
        this.confirmationTokens = confirmationTokens;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        confirmationTokens.ifAvailable(service ->
                registry.addInterceptor(new ConfirmationInterceptor(service)).addPathPatterns("/api/**"));
    }
}
