package com.example.boardproject.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(name = "hidden_community")
/*
 * 기능 : 사용자별 커뮤니티 숨김 데이터
 * 설명 : 사용자와 커뮤니티의 복합 키로 개인 표시 설정만 저장합니다. 커뮤니티나 게시글 자체를 삭제하지 않습니다.
 */
public class HiddenCommunity {
    @EmbeddedId
    private HiddenCommunityId id;
}
