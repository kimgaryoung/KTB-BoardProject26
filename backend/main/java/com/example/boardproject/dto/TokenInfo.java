package com.example.boardproject.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenInfo {

    private String accessToken;
    private long expiresIn;
}
