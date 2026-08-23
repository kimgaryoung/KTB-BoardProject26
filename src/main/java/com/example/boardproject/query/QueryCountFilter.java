package com.example.boardproject.query;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

// 게시글 목록/상세/검색 조회 요청 1건당 발생하는 쿼리 수(N+1 확인용)를 로그로 남기는 필터
@Slf4j //lombok이 log를 자동 생성해줌.
@Component //spring beam에 등록함.
@RequiredArgsConstructor //final 필드에 대한 생성자를 lombok이 자동 생성함.

public class QueryCountFilter
        extends OncePerRequestFilter {

    private static final Pattern POST_DETAIL_URI =
            Pattern.compile("^/posts/\\d+$");

    private final QueryCountInspector queryCountInspector;

    @Override
    protected boolean shouldNotFilter(
            @NonNull HttpServletRequest request
    ) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();

        boolean shouldFilter =
                "/posts".equals(path)
                        || "/posts/search".equals(path)
                        || POST_DETAIL_URI.matcher(path).matches();

        return !shouldFilter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        queryCountInspector.startCount();

        try {
            filterChain.doFilter(request, response);
        } finally {
            QueryCounter counter =
                    queryCountInspector.getQueryCount();

            if (counter != null) {
                log.info(
                        "[QueryCount] {} {} - 쿼리 수={}, 소요시간={}ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        counter.getQueryCount(),
                        counter.getElapsedMillis()
                );

                counter.getQueries().forEach(sql ->
                        log.debug("[Executed SQL] {}", sql)
                );
            }

            queryCountInspector.clearCounter();
        }
    }
}