package com.example.boardproject.config;

import com.example.boardproject.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/logout", "/token/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health", "/posts/**", "/users/email/check", "/users/nickname/check").permitAll()
                        .anyRequest().authenticated()
                )
                //  JWT 필터를 Security 체인에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:8080");  // 프론트 주소
        //http://board-scarlettproject-alb-1578950709.ap-northeast-2.elb.amazonaws.com/html/index.html

        config.addAllowedOrigin("http://board-scarlettproject-alb-1578950709.ap-northeast-2.elb.amazonaws.com/html/index.html");  // ALB주소

        config.addAllowedMethod("*");   // GET, POST, PUT, DELETE, PATCH 모두 허용
        config.addAllowedHeader("*");   // 모든 헤더 허용
        config.setAllowCredentials(true);  // 쿠키 전송 허용 (credentials: 'include' 때문에 필요)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
