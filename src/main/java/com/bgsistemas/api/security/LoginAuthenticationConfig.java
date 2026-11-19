package com.bgsistemas.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

// BGSI-238: the login of Spring Security, but checking the account status after the password
@Configuration
public class LoginAuthenticationConfig {

    @Bean
    DaoAuthenticationProvider loginAuthenticationProvider(JpaUserDetailsService userDetailsService,
                                                          PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(details -> { }); // nothing before the password...
        provider.setPostAuthenticationChecks(new LoginStatusChecker()); // ...the status after it
        return provider;
    }
}
