package com.example.boardproject.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;


//spring bean 등록 : JwtProvider개체들 jwtProperties의 값들 DI로 받아서 사용하려고
@Component

//final필드를 받는 jwtProperties의 생성자 자동으로 만들어줌.
@RequiredArgsConstructor
public class JwtProvider {

    //jwt의 설정값.
    private final JwtProperties jwtProperties;
    //서명용키
    private Key key;

    //1. DI후 자동으로 .  @PostConstruct가 붙은 init()실행.
    @PostConstruct
    public void init() {
        //설정 파일에 있던 secret값을 가져온걸로 getBytes()로 배열로 바꾸고
        //hmacShaKeyFor()로 서명(비밀키)를 반환해줌.
        this.key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }


    //2. refresh token과 access token 둘다 생성하는 함수
    private String createToken(

           //tpye: refresh인지 access인지
            String type,
            //user의 pk(user_id)
            Long user_id,
            //주의 :토큰은 인코딩만 되기 때문에 여기 민감한 점보 담으면 안됨.
           // 토큰에 추가로 담을 정보 또는 권한 정보 담김.
            Map<String, Object> claims,
            //토큰 만료시간
            long expireAt
    ) {
        //현재 시간을 가져옴(토큰 발금과 만료시간 계산할때 사용)
        Instant now = Instant.now();

        // claims에 typ 추가해서 덮어씌워지는 문제 해결
        Map<String, Object> allClaims = new java.util.HashMap<>(claims);
        allClaims.put("typ", type);

        //빌더로 토큰 정보 조함 (heager +payload +Signature 구조 만들기)
        return Jwts.builder()
                .subject(String.valueOf(user_id))
                .claims(allClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireAt)))
                .signWith((SecretKey) key, Jwts.SIG.HS256)
                .compact();
    }


    //3. AccessToken 발급하기
    public String createAccessToken(Long user_id) {
        return createToken(
                "access",
                user_id,

                //자주 조회되는 값인 닉네임과 프로필 사진을 AccessToken에 넣어줌
                Map.of(
                        /*UserProfileRepository가 추가로 필요해서 뺌, 용량도 커져서
                        "nickname", nickname,
                        "profile_image", profile_image

                         */
                ),
                jwtProperties.getAccessTokenExpSeconds()
        );
    }


    //4.RefreshToken 발급하기
    public String createRefreshToken(Long user_id) {
        return createToken(
                "refresh",
                user_id,
                Map.of(),
                jwtProperties.getRefreshTokenExpSeconds()
        );
    }


    //5.서명이랑 페이로드 검사 - 실패하면 인증 실패.
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                // 검증할 비밍키 등록.
                .verifyWith((SecretKey) key)
                .build()
                //서명검증 - 만료시간, 내가 만든건지, 중간데 payload 변경 안되었는지.
                .parseSignedClaims(token);
    }

    //6.AccessToken인지 아닌지 확인
    public boolean isAccessToken(String token) {
        return "access".equals(parse(token).getPayload().get("typ", String.class));
    }

    //7.서버에서 전송된 user_id를 꺼내는 메서드 - 현재 로그인한 사용자 찾으려고 사용함.
    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getPayload().getSubject());
    }

    //.초 -> 밀리초 로 바꾸는 함수 (쿠키 만료시간 정할때 ms 씀)
    public Long getAccessTokenValidityInMilliseconds() {
        return jwtProperties.getAccessTokenExpSeconds() * 1000;
    }
}
