package com.example.boardproject.repository;


//JPA가 제공하는 기본 CRUD모음.-SQL문 안짜도 됨
import com.example.boardproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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


}