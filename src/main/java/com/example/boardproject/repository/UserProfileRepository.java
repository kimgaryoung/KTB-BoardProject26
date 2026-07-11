package com.example.boardproject.repository;
import com.example.boardproject.entity.Comment;
import com.example.boardproject.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long>{

    @Query("select u from UserProfile u where u.userId = :userId")
    UserProfile findByUserId(Long userId);

    @Query("select c from Comment c where  c.commentId=:commentId")
    Comment findByCommentId(Integer commentId);

    boolean existsByNickname(String nickname);
}
