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
    /*
     * 기능 : 공개 읽기와 로그인 필수 작업의 접근 정책
     * 설명 : 가입·로그인과 공개 GET은 허용하고 나머지 요청은 인증을 요구합니다. 세션 없이 JWT 필터로 글 작성 등의 접근을 검사합니다.
     */
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/logout", "/token/refresh", "/users/upload/profile-image").permitAll()
                        .requestMatchers(HttpMethod.GET, "/site/board", "/health", "/posts/**", "/images/**", "/communities", "/communities/**", "/users/email/check", "/users/nickname/check").permitAll()
                        .anyRequest().authenticated()
                )
                //  JWT 필터를 Security 체인에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    /*
     * 기능 : 프론트와 백엔드 사이 CORS 설정
     * 설명 : 허용된 프론트 주소의 메서드·헤더와 인증 쿠키 전송을 허용합니다.
     */
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:8080");  // 프론트 주소

        config.addAllowedOrigin("http://16.184.42.190:8080");
        //http://board-scarlettproject-alb-1578950709.ap-northeast-2.elb.amazonaws.com/html/index.html

        //config.addAllowedOrigin("http://board-scarlettproject-alb-1578950709.ap-northeast-2.elb.amazonaws.com/html/index.html");  // ALB주소

        config.addAllowedMethod("*");   // GET, POST, PUT, DELETE, PATCH 모두 허용
        config.addAllowedHeader("*");   // 모든 헤더 허용
        config.setAllowCredentials(true);  // 쿠키 전송 허용 (credentials: 'include' 때문에 필요)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
