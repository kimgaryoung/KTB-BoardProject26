package com.example.boardproject.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSignRequestDto {

    // 1. 회원가입시  입력하는 값.
    private String email;
    private String password;
    private String nickname;

    // 2. 프로필 사진은 필수값 아님
    private String profileImage;
}
