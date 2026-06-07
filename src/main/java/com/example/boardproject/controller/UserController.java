package com.example.boardproject.controller;


import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.dto.*;
import com.example.boardproject.service.UserService;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor

public class UserController {

    private final UserService userService;

    //1. 회원가입
    @PostMapping("/users")
    public ResponseEntity<Void> signup(
            @Valid @RequestBody UserSignRequestDto signupRequest
    ) {
        userService.signup(signupRequest);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 2.로그인
    @PostMapping("/auth")
    public ResponseEntity<UserLoginResponseDto> login(
            @Valid @RequestBody UserLoginRequestDto loginRequest,
            HttpServletResponse httpResponse
    ) {
        //3. 로그인 처리
        UserLoginResult result = userService.login(loginRequest);

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(14 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        //4. 쿠키를 응답 헤더에 추가
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body( result.getResponse());
    }

    //5. 액세스 토큰 재발급
    @PostMapping("/token/refresh")
    public ResponseEntity<TokenInfo> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse
    ) {
        TokenResult result = userService.refreshAccessToken(refreshToken);

        // 6.Refresh Token 회전 시 새 쿠키 세팅
        if (result.getNewRefreshToken() != null) {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", result.getNewRefreshToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(14 * 24 * 60 * 60)
                    .sameSite("Lax")
                    .build();
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(result.getToken());
    }

    //회원정보 수정- 프로필 사진, 닉네임.
    //권한이 있는 회원만 수정해야하니까 @AuthenticationPrincipal final PrincipalDetails user 사용
    @PatchMapping("/users/profile")
    public String updateProfile(@RequestBody final UserProfileRequestDto dto,
                                @AuthenticationPrincipal final PrincipalDetails user) throws Exception {


        return userService.updateProfile(user.getId(), dto);
    }

    //회원 정보 수정 - 비밀번호
    //권한이 있는 회원만 수정해야하니까 @AuthenticationPrincipal final PrincipalDetails user 사용
    @PatchMapping("/users/password")
    public String updatePassword(@RequestBody final UserProfileRequestDto dto,
                                 @AuthenticationPrincipal final PrincipalDetails user) throws Exception {


        return userService.updatePassword(user.getId(), dto);
    }

    //회원 탈퇴
    @DeleteMapping("/users/{userId}")
    public String deleteUser(@PathVariable("userId") final Long userId) throws Exception {

        return userService.deleteUser(userId);
    }

}




