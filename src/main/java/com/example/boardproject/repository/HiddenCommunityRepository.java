package com.example.boardproject.repository;

import com.example.boardproject.entity.HiddenCommunity;
import com.example.boardproject.entity.HiddenCommunityId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface HiddenCommunityRepository extends JpaRepository<HiddenCommunity, HiddenCommunityId> {
    @Modifying
    @Query(value = "INSERT IGNORE INTO hidden_community (user_id, community_id) VALUES (:userId, :communityId)", nativeQuery = true)
    /*
     * 기능 : 개인 숨김 기록 저장
     * 설명 : INSERT IGNORE로 이미 숨긴 커뮤니티에 대한 반복 요청도 중복 오류 없이 처리합니다.
     */
    void hide(@Param("userId") Long userId, @Param("communityId") Long communityId);

    @Modifying
    @Query("DELETE FROM HiddenCommunity h WHERE h.id.userId = :userId AND h.id.communityId = :communityId")
    /*
     * 기능 : 개인 숨김 기록 해제
     * 설명 : 사용자와 커뮤니티 ID가 모두 일치하는 숨김 행만 삭제합니다. 이미 복구된 경우도 처리할 수 있습니다.
     */
    void show(@Param("userId") Long userId, @Param("communityId") Long communityId);
}
