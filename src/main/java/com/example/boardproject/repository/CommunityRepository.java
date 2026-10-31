package com.example.boardproject.repository;

import com.example.boardproject.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    /*
     * 기능 : 이름 비교 키로 커뮤니티 조회
     * 설명 : 정규화한 키의 동일 여부로 중복 커뮤니티를 찾습니다.
     */
    Optional<Community> findByNameKey(String nameKey);

    /*
     * 기능 : 숨김 필터를 먼저 적용하는 커뮤니티 검색 쿼리
     * 설명 : 사용자별 숨김 포함·제외와 이름 부분 검색을 DB에서 처리한 뒤 최신 ID 순으로 limit·offset을 적용합니다.
     */
    @Query(value = """
            SELECT * FROM community c
            WHERE LOCATE(:keyword, c.name_key) > 0
              AND ((:hiddenOnly = FALSE AND NOT EXISTS (
                    SELECT 1 FROM hidden_community h WHERE h.user_id = :userId AND h.community_id = c.id
                  )) OR (:hiddenOnly = TRUE AND EXISTS (
                    SELECT 1 FROM hidden_community h WHERE h.user_id = :userId AND h.community_id = c.id
                  )))
            ORDER BY c.id DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Community> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit,
                           @Param("userId") Long userId, @Param("hiddenOnly") boolean hiddenOnly);
}
