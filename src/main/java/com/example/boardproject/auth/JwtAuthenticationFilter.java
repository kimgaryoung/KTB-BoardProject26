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
            "/users/upload/profile-image",
    };

    //부모 클래스에서도 protected로 해서 접근제어자 이럼.
    //2.WHITE_LIST에 있는 url은 토큰 검증 없이 건너뜀
    @Override
    /*
     * 기능 : 토큰 검사 제외 경로 판정
     * 설명 : 로그인·가입·중복 확인·토큰 재발급 등 허용된 인증 진입 경로는 JWT 검사에서 제외합니다.
     */
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        String uri = request.getServletPath();
        return PatternMatchUtils.simpleMatch(WHITE_LIST, uri);
    }

    // GET /posts, GET /posts/1 는 토큰이 없어도(비로그인) 통과시키되,
    // 토큰이 있으면 파싱해서 principal을 채워야 조회수 증가 등 로그인 전용 로직이 동작함.
    /*
     * 기능 : 비로그인 조회 허용 경로 판정
     * 설명 : 게시글·이미지·커뮤니티 GET 요청은 토큰 없이도 통과시키고, 토큰이 있으면 사용자 정보를 채울 수 있게 합니다.
     */
    private boolean isOptionalAuth(HttpServletRequest request) {
        return request.getMethod().equals("GET")
                && PatternMatchUtils.simpleMatch(new String[]{"/site/board", "/posts*", "/images/*", "/communities", "/communities/*"}, request.getServletPath());
    }


    //3.실제 토큰 검증 로직 -모든 요청마다 실행함.
    @Override
    /*
     * 기능 : JWT 인증과 로그인 필수 요청 차단
     * 설명 : Bearer 헤더 또는 accessToken 쿠키를 검증해 SecurityContext에 사용자를 등록합니다. 인증 필수 요청의 누락·만료·잘못된 토큰은 HTTP 401로 차단합니다.
     */
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

        boolean optionalAuth = isOptionalAuth(request);

        if (token == null) {
            if (optionalAuth) {
                filterChain.doFilter(request, response);
                return;
            }
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

            if (optionalAuth) {
                filterChain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
