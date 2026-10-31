package com.example.boardproject.controller;

import com.example.boardproject.auth.PrincipalDetails;
import com.example.boardproject.auth.Result;
import com.example.boardproject.dto.*;
import com.example.boardproject.service.UserService;
import com.example.boardproject.service.PostImageStorage;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostImageStorage images;

    /*
     * 기능 : 회원가입·회원정보 수정용 프로필 사진 업로드
     * 설명 : 공통 저장소에서 실제 이미지 형식과 5MB 제한을 검사합니다. 반환된 주소를 가입 또는 내 정보 저장 시 연결합니다.
     */
    @PostMapping(value = "/users/upload/profile-image", consumes = "multipart/form-data")
    public ResponseEntity<Result<Map<String, String>>> uploadProfileImage(@RequestPart("profileImage") MultipartFile image) {
        String url = images.getImageUrl(images.upload(image));
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.of(Map.of("profileImageUrl", url)));
    }

    // 회원가입
    @PostMapping("/auth/signup")
    /*
     * 기능 : 회원가입 API
     * 설명 : 가입 정보를 서비스에 전달하고 계정 생성 시 HTTP 201을 반환합니다.
     */
    public ResponseEntity<Void> signup(
            @RequestBody UserSignRequestDto signupRequest
    ) {
        userService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 로그인
    @PostMapping("/auth/login")
    /*
     * 기능 : 로그인 API와 인증 쿠키 발급
     * 설명 : 로그인 성공 시 accessToken·refreshToken을 HttpOnly 쿠키에 설정하고 로그인 응답을 반환합니다.
     */
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
    /*
     * 기능 : 현재 세션 확인 API
     * 설명 : 보안 필터가 확인한 사용자의 계정·프로필 정보를 반환합니다.
     */
    public ResponseEntity<Result<AuthCheckResponseDto>> authCheck(
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        AuthCheckResponseDto response = userService.getAuthCheck(user.getId());
        return ResponseEntity.ok(Result.of(response));
    }

    // 이메일 중복 확인
    @GetMapping("/users/email/check")
    /*
     * 기능 : 이메일 중복 확인 API
     * 설명 : 사용 가능하면 HTTP 200, 이미 존재하면 HTTP 409를 반환합니다.
     */
    public ResponseEntity<Void> checkEmail(@RequestParam String email) {
        if (userService.isEmailAvailable(email)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    // 닉네임 중복 확인
    @GetMapping("/users/nickname/check")
    /*
     * 기능 : 닉네임 중복 확인 API
     * 설명 : 사용 가능하면 HTTP 200, 이미 존재하면 HTTP 409를 반환합니다.
     */
    public ResponseEntity<Void> checkNickname(@RequestParam String nickname) {
        if (userService.isNicknameAvailable(nickname)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    // 액세스 토큰 재발급
    @PostMapping("/token/refresh")
    /*
     * 기능 : 토큰 갱신 API
     * 설명 : 쿠키의 refreshToken으로 새 토큰을 발급하고 새 refreshToken 쿠키와 토큰 응답을 반환합니다.
     */
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
    /*
     * 기능 : 내 프로필 수정 API
     * 설명 : 인증된 사용자 ID로 닉네임과 사진 주소의 변경을 요청합니다.
     */
    public String updateProfile(@RequestBody final UserProfileRequestDto dto,
                                @AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        return userService.updateProfile(user.getId(), dto);
    }

    // 비밀번호 변경
    @PatchMapping("/users/me/password")
    /*
     * 기능 : 내 비밀번호 변경 API
     * 설명 : 인증된 사용자 ID와 새 비밀번호 정보를 서비스에 전달합니다.
     */
    public String updatePassword(@RequestBody final UserProfileRequestDto dto,
                                 @AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        return userService.updatePassword(user.getId(), dto);
    }

    // 회원 탈퇴
    @DeleteMapping("/users/me")
    /*
     * 기능 : 회원 탈퇴 API
     * 설명 : 인증된 계정의 삭제를 요청합니다.
     */
    public String deleteUser(@AuthenticationPrincipal final PrincipalDetails user) throws Exception {
        return userService.deleteUser(user.getId());
    }

    // 로그아웃
    @PostMapping("/auth/logout")
    /*
     * 기능 : 로그아웃 API
     * 설명 : accessToken과 refreshToken 쿠키의 만료 시간을 0으로 설정해 브라우저의 인증 쿠키를 지웁니다.
     */
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
