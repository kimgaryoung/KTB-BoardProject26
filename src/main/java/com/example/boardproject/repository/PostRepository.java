package com.example.boardproject.repository;

//JPA가 제공하는 기본 CRUD모음 - SQL문 안짜도 됨
import com.example.boardproject.entity.Post;

import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
// Post 테이블 기본 CRUD  자동 제공
public interface PostRepository extends JpaRepository<Post, Long> {
    // 게시글 목록 전체 조회 - findAll() 은 JpaRepository에 이미 내장되어 있어서 따로 안 써도 됨


    // postid조회 함수로 사용.-게시글삭제, 수정.
    @Query("select p from Post p where p.postId = :postId")
    Post findByPostId(Long postId);

    @Query("select p from Post p where (:cursor is null or p.postId < :cursor) order by p.postId desc")
    List<Post> findAllByCursor(@org.springframework.data.repository.query.Param("cursor") Long cursor,
                                org.springframework.data.domain.Pageable pageable);

    //게시글 목록 조회 - cusor방식
    @Query("""
        SELECT p
        FROM Post p 
        WHERE :cursor IS NULL
                OR p.postId < :cursor
        ORDER BY p.postId DESC
        """)
    //Slice와 list와 page차이 : slice 다음페이지 존재 여부를 +1개를 더 페이지 해와서 판단함. list :현재데이터만,page : 다음 페이지 여부 + 전체 개수
    Slice<Post> findPostByCursor(
      @Param("cursor")Long cursor,
      Pageable pageable
    );

    // 게시글 검색 조회 - offset/limit 방식
    @Query(value = """
        SELECT *
        FROM POST p
        WHERE p.post_title LIKE CONCAT('%', :keyword, '%')
           OR p.post_content LIKE CONCAT('%', :keyword, '%')
        ORDER BY p.post_date DESC, p.post_id DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Post> findSearchPostsByOffset(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

}
