package com.example.boardproject.service;

import com.example.boardproject.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class ImageUploadValidator {
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    record Image(byte[] bytes, String extension, String contentType) {}

    /*
     * 기능 : 업로드 이미지 크기와 형식 검사
     * 설명 : 빈 파일과 5MB 초과 파일을 거부합니다. 파일명·요청 MIME 대신 실제 바이트 시그니처로 확장자와 응답 MIME을 결정합니다.
     */
    static Image validate(MultipartFile file) {
        if (file.isEmpty()) throw invalid("EMPTY_IMAGE");
        if (file.getSize() > MAX_BYTES) throw new BusinessException("IMAGE_TOO_LARGE", HttpStatus.valueOf(413));
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_BYTES) throw new BusinessException("IMAGE_TOO_LARGE", HttpStatus.valueOf(413));
            // Derive the served type and extension from bytes, not the supplied filename or MIME.
            if (starts(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a) && bytes.length >= 24) return new Image(bytes, "png", "image/png");
            if (starts(bytes, 0xff, 0xd8, 0xff)) return new Image(bytes, "jpg", "image/jpeg");
            if (textAt(bytes, 0, "GIF87a") || textAt(bytes, 0, "GIF89a")) return new Image(bytes, "gif", "image/gif");
            if (textAt(bytes, 0, "RIFF") && textAt(bytes, 8, "WEBP") && textAt(bytes, 12, "VP8")) return new Image(bytes, "webp", "image/webp");
            if (starts(bytes, 0x42, 0x4d) && bytes.length >= 26) return new Image(bytes, "bmp", "image/bmp");
            throw invalid("INVALID_IMAGE_FORMAT");
        } catch (IOException exception) { throw invalid("IMAGE_READ_FAILED"); }
    }

    /*
     * 기능 : 이미지 시작 바이트 비교
     * 설명 : 파일 앞부분이 PNG·JPEG 등의 시그니처와 일치하는지 확인합니다.
     */
    private static boolean starts(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if ((bytes[i] & 0xff) != prefix[i]) return false;
        return true;
    }
    /*
     * 기능 : 이미지 내부 형식 식별자 확인
     * 설명 : 지정 위치의 ASCII 식별자를 비교해 GIF·WebP 등의 형식을 판별합니다.
     */
    private static boolean textAt(byte[] bytes, int offset, String value) {
        byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) if (bytes[offset + i] != expected[i]) return false;
        return true;
    }
    private static BusinessException invalid(String code) { return new BusinessException(code, HttpStatus.BAD_REQUEST); }
}
