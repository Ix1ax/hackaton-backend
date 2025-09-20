package ru.ixlax.hackaton.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:3000",
                "https://9dae7702bf7f21.lhr.life",
                "https://05819e8f107e07.lhr.life"
        ));
        c.setAllowCredentials(true);
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("*")); // на всякий

        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    @Override
    public void addCorsMappings(CorsRegistry r) {
        r.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:3000",
                        "https://9dae7702bf7f21.lhr.life",
                        "https://05819e8f107e07.lhr.life"
                )
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/public/**","/ws/**","/actuator/**").permitAll()
                        .anyRequest().permitAll()
                )
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .build();
    }
}