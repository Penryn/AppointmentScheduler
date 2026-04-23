package com.example.slabiak.appointmentscheduler.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final PasswordEncoder passwordEncoder;

    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService, CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler, PasswordEncoder passwordEncoder) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/metrics/**", "/actuator/prometheus").permitAll()
                        .requestMatchers("/customers/new/**").permitAll()
                        .requestMatchers("/").hasAnyRole("CUSTOMER", "PROVIDER", "ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("CUSTOMER", "PROVIDER", "ADMIN")
                        .requestMatchers("/customers/all").hasRole("ADMIN")
                        .requestMatchers("/providers/new").hasRole("ADMIN")
                        .requestMatchers("/invoices/all").hasRole("ADMIN")
                        .requestMatchers("/providers/all").hasRole("ADMIN")
                        .requestMatchers("/customers/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/providers/availability/**").hasRole("PROVIDER")
                        .requestMatchers("/providers/**").hasAnyRole("PROVIDER", "ADMIN")
                        .requestMatchers("/works/**").hasRole("ADMIN")
                        .requestMatchers("/exchange/**").hasRole("CUSTOMER")
                        .requestMatchers("/appointments/new/**").hasRole("CUSTOMER")
                        .requestMatchers("/appointments/**").hasAnyRole("CUSTOMER", "PROVIDER", "ADMIN")
                        .requestMatchers("/invoices/**").hasAnyRole("CUSTOMER", "PROVIDER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/perform_logout"))
                .exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedPage("/access-denied"));
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(customUserDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }
}
