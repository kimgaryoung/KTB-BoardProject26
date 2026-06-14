package com.example.boardproject.auth;

import com.example.boardproject.entity.User;
import com.example.boardproject.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor

//1.request에 한번만 실행되는 필터.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    //토큰 검증 없이 통과시킬 url
    private static final String[] WHITE_LIST = {
            "/auth/signup",
            "/auth/login",
            "/auth/logout",
            "/token/refresh",
            "/users/email/check",
            "/users/nickname/check",
    };

    //부모 클래스에서도 protected로 해서 접근제어자 이럼.
    //2.WHITE_LIST에 있는 url은 토큰 검증 없이 건너뜀
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        String uri = request.getServletPath();
        String method = request.getMethod();

        // GET /posts, GET /posts/1 는 토큰 없이 통과
        if (method.equals("GET") && PatternMatchUtils.simpleMatch("/posts*", uri)) {
            return true;
        }
        return PatternMatchUtils.simpleMatch(WHITE_LIST, uri);
    }




    //3.실제 토큰 검증 로직 -모든 요청마다 실행함.
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            jwtProvider.parse(token);

            if (!jwtProvider.isAccessToken(token)) {
                throw new IllegalArgumentException("Not access token");
            }

            //  추가: 토큰에서 userId 꺼내서 SecurityContext에 저장
            String userId = jwtProvider.getUserId(token).toString(); // JwtProvider에 이 메서드 있어야 함
            User user = userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            PrincipalDetails principalDetails = new PrincipalDetails(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principalDetails, null, principalDetails.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception exception) {
            //System.out.println("JWT 필터 예외: " + exception.getMessage()); // 이미 있으면 그대로

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}