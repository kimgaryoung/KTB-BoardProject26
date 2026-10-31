package com.example.boardproject.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

//  예외 처리 - 컨트롤러에서 터진 예외를 여기서 잡아서 적절한 HTTP res 변환
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    /*
     * 기능 : 업로드 제한 초과 오류 응답
     * 설명 : multipart 용량 제한을 넘으면 IMAGE_TOO_LARGE와 HTTP 413을 반환합니다.
     */
    public ResponseEntity<Map<String, String>> handleImageTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(413).body(Map.of("code", "IMAGE_TOO_LARGE"));
    }

    @ExceptionHandler(CommunityAlreadyExistsException.class)
    /*
     * 기능 : 커뮤니티 중복 생성 오류 응답
     * 설명 : HTTP 409와 기존 커뮤니티 ID를 반환해 화면에서 해당 커뮤니티로 안내할 수 있게 합니다.
     */
    public ResponseEntity<Map<String, Object>> handleCommunityAlreadyExists(CommunityAlreadyExistsException e) {
        return ResponseEntity.status(409).body(Map.of("code", "COMMUNITY_ALREADY_EXISTS", "communityId", e.getCommunityId()));
    }

    // BusinessException -  status 코드 응답
    @ExceptionHandler(BusinessException.class)
    /*
     * 기능 : 업무 오류의 공통 HTTP 응답
     * 설명 : 서비스에서 지정한 상태 코드와 오류 코드를 유지해 JSON으로 반환합니다.
     */
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getStatus())
                .body(Map.of("code", e.getCode()));
    }
}
