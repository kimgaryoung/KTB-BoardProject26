package com.example.boardproject.service;

import com.example.boardproject.dto.CommunityResponseDto;
import com.example.boardproject.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteBoardService {
    private final CommunityService communities;
    @Value("${app.site.community-management-enabled:false}")
    private boolean legacyManagement;

    /*
     * 기능 : 서버 시작 시 전용 게시판 준비
     * 설명 : HTTP 요청을 받기 전에 생성합니다. 서버 여러 대의 초기화가 겹치면 독립 트랜잭션 종료 후 다시 조회합니다.
     */
    @PostConstruct
    public void initializeBoard() {
        try { communities.getOrCreateMinecraftBoard(); }
        catch (DataIntegrityViolationException race) { communities.getOrCreateMinecraftBoard(); }
    }

    /*
     * 기능 : ID를 하드코딩하지 않고 전용 게시판 조회
     * 설명 : 일반 조회는 기존 트랜잭션에 참여해 요청당 추가 DB 연결을 점유하지 않습니다.
     */
    public CommunityResponseDto getBoard() {
        return communities.findByName("마인크래프트")
                .orElseThrow(() -> new IllegalStateException("마인크래프트 게시판이 초기화되지 않았습니다."));
    }

    /*
     * 기능 : 게시글 접근을 마인크래프트 게시판으로 제한
     * 설명 : 일반 서비스에서는 소속 생략도 전용 게시판으로 처리하고 다른 소속은 404로 거부합니다. 명시적 호환 모드는 기존 데이터 회귀 테스트에만 사용합니다.
     */
    public Long resolveCommunityId(Long requested) {
        if (legacyManagement) return requested;
        Long siteId = getBoard().id();
        if (requested != null && !requested.equals(siteId)) {
            throw new BusinessException("POST_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        return siteId;
    }
}
