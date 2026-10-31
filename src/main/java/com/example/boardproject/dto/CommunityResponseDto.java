package com.example.boardproject.dto;

import com.example.boardproject.entity.Community;

/*
 * 기능 : 커뮤니티 탐색 응답 데이터
 * 설명 : 이름·소개와 이동할 커뮤니티 ID를 반환하며 내부 중복 키나 생성자 정보는 노출하지 않습니다.
 */
public record CommunityResponseDto(Long id, String name, String description) {
    public static CommunityResponseDto from(Community community) {
        return new CommunityResponseDto(community.getId(), community.getName(), community.getDescription());
    }
}
