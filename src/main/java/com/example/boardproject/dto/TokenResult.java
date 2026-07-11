package com.example.boardproject.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;


//역할 - 토큰 재발급 결과 처리  결과 처리
@Getter
@AllArgsConstructor
public class TokenResult {

    //1. 응답 바디 (새 accessToken + 만료시간)
    private TokenInfo token;


    //2.  RTR 발생 시 새 refreshToken (없으면 null)
    private String newRefreshToken;





}
