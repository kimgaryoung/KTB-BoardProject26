package com.example.boardproject.repository;


//JPA가 제공하는 기본 CRUD모음.-SQL문 안짜도 됨
import com.example.boardproject.entity.User;
import com.example.boardproject.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 회원가입시 -메일 중복체크 ,로그인 시- 메일 사용
    Optional<User> findByEmail(String email);

    // 로그인시 -토근 만료되서 재발급할떄 refreshToken으로 찾기
    Optional<User> findByRefreshToken(String refreshToken);

    //이메일 중복 검사
    boolean existsByEmail(String email);

    //비밀번호 수정할때 userid조회 함수로 사용.
    @Query("select u from User u where u.userId = :userId")
    User findByUserId(Long userId);


}