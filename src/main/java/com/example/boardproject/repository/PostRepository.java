package com.example.boardproject.repository;

//JPA가 제공하는 기본 CRUD모음 - SQL문 안짜도 됨
import com.example.boardproject.entity.Post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
// Post 테이블 기본 CRUD  자동 제공
public interface PostRepository extends JpaRepository<Post, Long> {
    // 게시글 목록 전체 조회 - findAll() 은 JpaRepository에 이미 내장되어 있어서 따로 안 써도 됨


    // postid조회 함수로 사용.-게시글삭제, 수정.
    @Query("select p from Post p where p.postId = :postId")
    Post findByPostId(Long postId);







}
