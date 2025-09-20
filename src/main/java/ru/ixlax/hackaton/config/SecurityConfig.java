package ru.ixlax.hackaton.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// ничего не делаем здесь
public class SecurityConfig {
    // пусто
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(c -> {}) // важно, чтобы подтянулся CorsConfigurationSource
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/public/**","/ws/**","/actuator/**").permitAll()
                        .anyRequest().permitAll()
                )
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .build();
    }
}