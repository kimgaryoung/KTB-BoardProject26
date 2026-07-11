package java.com.example.boardproject.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

//  예외 처리 - 컨트롤러에서 터진 예외를 여기서 잡아서 적절한 HTTP res 변환
@RestControllerAdvice
public class GlobalExceptionHandler {

    // BusinessException -  status 코드 응답
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getStatus())
                .body(Map.of("code", e.getCode()));
    }
}
