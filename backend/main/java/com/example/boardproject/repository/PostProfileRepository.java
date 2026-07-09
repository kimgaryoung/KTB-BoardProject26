package com.example.boardproject.repository;

import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.PostProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostProfileRepository extends JpaRepository<PostProfile, Long> {
    PostProfile findByPostId(Long postId);


    //@mapId 덕분에 쿼리 필요 없음.
    // postid조회- 어떤 게시글의 댓글수인지
//    @Query("select p from PostProfile p where p.postId = :postId")
//    Post findByPostId(Long postId);
//











}
