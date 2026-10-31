package com.example.boardproject.dto;

/*
 * 기능 : 개인 숨김 변경 결과
 * 설명 : 대상 커뮤니티 ID와 변경 후 숨김 여부를 반환합니다.
 */
public record CommunityVisibilityResponseDto(Long communityId, boolean hidden) {}
