package com.example.boardproject.controller;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.auth.Result;
import com.example.boardproject.dto.*;
import com.example.boardproject.exception.CommunityAlreadyExistsException;
import com.example.boardproject.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/communities")
@ConditionalOnProperty(name = "app.site.community-management-enabled", havingValue = "true")
public class CommunityController {
    private final CommunityService communityService;

    @GetMapping
    /*
     * 기능 : 커뮤니티 검색 API
     * 설명 : 검색어·페이지·숨김 모드와 인증된 사용자 ID를 서비스에 전달합니다. 비로그인도 일반 검색을 이용할 수 있습니다.
     */
    public Result<List<CommunityResponseDto>> search(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean hidden,
            @AuthenticationPrincipal PrincipalDetails principal) {
        return Result.of(communityService.search(keyword, offset, limit, principal == null ? null : principal.getId(), hidden));
    }

    @PutMapping("/{id}/hidden")
    /*
     * 기능 : 내 목록에서 커뮤니티 숨기기 API
     * 설명 : 인증 정보의 사용자 ID만 사용해 해당 계정의 숨김 설정을 저장합니다.
     */
    public Result<CommunityVisibilityResponseDto> hide(@PathVariable Long id, @AuthenticationPrincipal PrincipalDetails principal) {
        return Result.of(communityService.setHidden(id, principal.getId(), true));
    }

    @DeleteMapping("/{id}/hidden")
    /*
     * 기능 : 숨긴 커뮤니티 다시 표시 API
     * 설명 : 인증된 계정의 숨김 설정을 해제합니다. 다른 사용자의 목록에는 영향을 주지 않습니다.
     */
    public Result<CommunityVisibilityResponseDto> show(@PathVariable Long id, @AuthenticationPrincipal PrincipalDetails principal) {
        return Result.of(communityService.setHidden(id, principal.getId(), false));
    }

    @GetMapping("/{id}")
    /*
     * 기능 : 커뮤니티 정보 조회 API
     * 설명 : 커뮤니티 이름과 소개를 공통 응답 형식으로 반환합니다.
     */
    public Result<CommunityResponseDto> getCommunity(@PathVariable Long id) { return Result.of(communityService.getCommunity(id)); }

    @PostMapping
    /*
     * 기능 : 커뮤니티 생성 API와 중복 충돌 처리
     * 설명 : 로그인 사용자를 생성자로 저장합니다. 동시 요청으로 이름이 충돌하면 종료된 트랜잭션 밖에서 기존 커뮤니티를 찾아 409 처리로 연결합니다.
     */
    public ResponseEntity<Result<CommunityResponseDto>> create(@RequestBody CommunityRequestDto request,
            @AuthenticationPrincipal PrincipalDetails principal) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Result.of(communityService.create(request, principal.getId())));
        } catch (DataIntegrityViolationException exception) {
            // The failed transaction has ended; resolve races against the unique name key in a fresh read.
            var existing = communityService.findByName(request.getName());
            if (existing.isPresent()) throw new CommunityAlreadyExistsException(existing.get().id());
            throw exception;
        }
    }
}
