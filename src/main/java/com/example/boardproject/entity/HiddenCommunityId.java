package com.example.boardproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
/*
 * 기능 : 개인 숨김 설정의 복합 식별자
 * 설명 : 사용자 ID와 커뮤니티 ID를 조합해 계정별 숨김을 구분합니다. 가상 자유게시판 ID 0도 사용할 수 있습니다.
 */
public class HiddenCommunityId implements Serializable {
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "community_id")
    private Long communityId;
}
