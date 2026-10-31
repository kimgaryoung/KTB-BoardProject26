package com.example.boardproject.auth;

import com.example.boardproject.entity.User;
import com.example.boardproject.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtProvider, userRepository);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPostDetail_withoutToken_continuesAsAnonymous() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/posts/1");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getPostDetail_withValidToken_populatesPrincipal() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/posts/1");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");

        User user = new User();
        ReflectionTestUtils.setField(user, "userId", 1L);

        when(jwtProvider.parse("valid-token")).thenReturn(mock(Jws.class));
        when(jwtProvider.isAccessToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        PrincipalDetails principal =
                (PrincipalDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getId()).isEqualTo(1L);
    }

    @Test
    void nonPostEndpoint_withoutToken_isRejected() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/users/me");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
