package com.example.boardproject.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserLoginResult {
    // 1.응답 바디 (accessToken, expiresIn)
    private UserLoginResponseDto response;

    // 2. 쿠키로 줄 refreshToken
    private String refreshToken;
}
