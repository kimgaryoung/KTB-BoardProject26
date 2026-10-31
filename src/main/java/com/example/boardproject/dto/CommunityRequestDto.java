package com.example.boardproject.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/*
 * 기능 : 커뮤니티 생성 요청 데이터
 * 설명 : 사용자가 입력한 커뮤니티 이름과 소개를 받습니다. 생성자 ID는 서버 인증 정보에서 결정합니다.
 */
public class CommunityRequestDto {
    private String name;
    private String description;
}
