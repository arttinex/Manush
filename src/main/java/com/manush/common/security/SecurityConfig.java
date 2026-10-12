package com.manush.common.security;

import com.manush.auth.service.GoogleOAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final GoogleOAuthSuccessHandler googleOAuthSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public API
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/ai/**",
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()
                // Public pages (client-side JS handles auth guards)
                .requestMatchers(
                    "/", "/index", "/about", "/contact",
                    "/feed", "/leaderboard", "/ai", "/jobs", "/blood", "/help", "/help/**",
                    "/jogajog", "/profile", "/community",
                    "/auth/login", "/auth/register", "/auth/callback", "/auth/complete-profile",
                    "/auth/verify-email", "/auth/forgot-password",
                    "/admin", "/admin/**"
                ).permitAll()
                // WebSocket endpoint handshake
                .requestMatchers("/ws/**").permitAll()
                // Static resources
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Actuator health
                .requestMatchers("/actuator/health").permitAll()
                // Swagger
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                // Read-only public feed, help requests, & public views
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/posts/**",
                    "/api/v1/jobs/**",
                    "/api/v1/leaderboard/**",
                    "/api/v1/community/**",
                    "/api/v1/help/**",
                    "/api/v1/help-requests/**"
                ).permitAll()
                // Admin API routes (strictly enforced by JWT Bearer token)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(googleOAuthSuccessHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
