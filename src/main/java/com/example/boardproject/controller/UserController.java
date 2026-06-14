package com.example.boardproject.controller;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.auth.Result;
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

    // 회원가입
    @PostMapping("/auth/signup")
    public ResponseEntity<Void> signup(
            @RequestBody UserSignRequestDto signupRequest
    ) {
        userService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 로그인
    @PostMapping("/auth/login")
    public ResponseEntity<UserLoginResponseDto> login(
            @Valid @RequestBody UserLoginRequestDto loginRequest,
            HttpServletResponse httpResponse
    ) {
        UserLoginResult result = userService.login(loginRequest);

        // refreshToken 쿠키
        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(14 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        // accessToken 쿠키 (프론트가 Bearer 헤더 대신 쿠키로 인증)
        ResponseCookie accessCookie = ResponseCookie
                .from("accessToken", result.getResponse().getToken().getAccessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(3 * 60 * 60)
                .sameSite("Strict")
                .build();

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.status(HttpStatus.OK).body(result.getResponse());
    }

    // 현재 로그인 유저 정보 조회 (모든 페이지에서 인증 확인용)
    @GetMapping("/auth/check")
    public ResponseEntity<Result<AuthCheckResponseDto>> authCheck(
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        AuthCheckResponseDto response = userService.getAuthCheck(user.getId());
        return ResponseEntity.ok(Result.of(response));
    }

    // 이메일 중복 확인
    @GetMapping("/users/email/check")
    public ResponseEntity<Void> checkEmail(@RequestParam String email) {
        if (userService.isEmailAvailable(email)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    // 닉네임 중복 확인
    @GetMapping("/users/nickname/check")
    public ResponseEntity<Void> checkNickname(@RequestParam String nickname) {
        if (userService.isNicknameAvailable(nickname)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    // 액세스 토큰 재발급
    @PostMapping("/token/refresh")
    public ResponseEntity<TokenInfo> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse
    ) {
        TokenResult result = userService.refreshAccessToken(refreshToken);

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

        return ResponseEntity.status(HttpStatus.OK).body(result.getToken());
    }

    // 회원정보 수정 (닉네임, 프로필 사진)
    @PutMapping("/users/me")
    public String updateProfile(@RequestBody final UserProfileRequestDto dto,
                                @AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        return userService.updateProfile(user.getId(), dto);
    }

    // 비밀번호 변경
    @PatchMapping("/users/me/password")
    public String updatePassword(@RequestBody final UserProfileRequestDto dto,
                                 @AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        return userService.updatePassword(user.getId(), dto);
    }

    // 회원 탈퇴
    @DeleteMapping("/users/me")
    public String deleteUser(@AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        return userService.deleteUser(user.getId());
    }

    // 로그아웃
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
                .httpOnly(true).secure(false).path("/").maxAge(0).build();
        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(false).path("/").maxAge(0).build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
        return ResponseEntity.ok().build();
    }
}
