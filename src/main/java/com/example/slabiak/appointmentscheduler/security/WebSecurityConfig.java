package com.example.slabiak.appointmentscheduler.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@EnableWebSecurity
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self' data:",
            "connect-src 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "object-src 'none'",
            "frame-src 'none'");

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final PasswordEncoder passwordEncoder;
    private final List<String> allowedCorsOrigins;

    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService,
                             CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.security.cors.allowed-origins}") List<String> allowedCorsOrigins) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.passwordEncoder = passwordEncoder;
        this.allowedCorsOrigins = allowedCorsOrigins.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProvider())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=(), fullscreen=(self)")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(EndpointRequest.to("health")).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedCorsOrigins);
        configuration.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(Arrays.asList(
                "Accept",
                "Authorization",
                "Content-Type",
                "X-CSRF-TOKEN",
                "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(customUserDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }
}
