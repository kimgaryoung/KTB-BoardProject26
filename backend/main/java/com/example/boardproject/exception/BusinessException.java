package com.example.boardproject.exception;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter@AllArgsConstructor
public class BusinessException extends RuntimeException {
    // HTTP 상태 코드 ex 401, 404 등
    private final HttpStatus status;

    // 에러 코드 ex NOT_FOUND
    private final String code;

    public BusinessException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
    }
}
