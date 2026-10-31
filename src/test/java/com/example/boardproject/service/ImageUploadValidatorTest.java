package com.example.boardproject.service;

import com.example.boardproject.exception.BusinessException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class ImageUploadValidatorTest {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/nKkAAAAASUVORK5CYII=");

    static Stream<Arguments> supportedSignatures() {
        return Stream.of(
                Arguments.of(PNG, "png", "image/png"),
                Arguments.of(HexFormat.of().parseHex("ffd8ffe000104a46494600"), "jpg", "image/jpeg"),
                Arguments.of(HexFormat.of().parseHex("47494638376101000100"), "gif", "image/gif"),
                Arguments.of(HexFormat.of().parseHex("47494638396101000100"), "gif", "image/gif"),
                Arguments.of(HexFormat.of().parseHex("5249464610000000574542505650384c00000000"), "webp", "image/webp"),
                Arguments.of(HexFormat.of().parseHex("424d000000000000000000000000000000000000000000000000"), "bmp", "image/bmp")
        );
    }

    @ParameterizedTest
    @MethodSource("supportedSignatures")
    void detectsSupportedSignatureInsteadOfTrustingFilenameOrMime(byte[] bytes, String extension, String contentType) {
        var result = ImageUploadValidator.validate(new MockMultipartFile(
                "profileImage", "untrusted.html", "text/html", bytes));

        assertThat(result.extension()).isEqualTo(extension);
        assertThat(result.contentType()).isEqualTo(contentType);
        assertThat(result.bytes()).isEqualTo(bytes);
    }

    @Test
    void rejectsAnEmptyFile() {
        assertRejected(new MockMultipartFile("profileImage", new byte[0]), "EMPTY_IMAGE", 400);
    }

    @Test
    void acceptsExactlyFiveMiB() {
        byte[] bytes = Arrays.copyOf(PNG, MAX_BYTES);
        var result = ImageUploadValidator.validate(new MockMultipartFile("profileImage", bytes));
        assertThat(result.bytes()).isEqualTo(bytes);
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsOneByteAboveFiveMiB() {
        assertRejected(new MockMultipartFile("profileImage", Arrays.copyOf(PNG, MAX_BYTES + 1)),
                "IMAGE_TOO_LARGE", 413);
    }

    @Test
    void rejectsOversizedBytesEvenIfReportedSizeIsSmall() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1L);
        when(file.getBytes()).thenReturn(Arrays.copyOf(PNG, MAX_BYTES + 1));
        assertRejected(file, "IMAGE_TOO_LARGE", 413);
    }

    static Stream<byte[]> invalidSignatures() {
        return Stream.of(
                "<svg onload='alert(1)'/>".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                Arrays.copyOf(PNG, 8),
                new byte[]{0x42, 0x4d},
                HexFormat.of().parseHex("524946460000000057415645565038")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidSignatures")
    void rejectsUnsupportedOrTruncatedSignaturesDespiteImageMime(byte[] bytes) {
        assertRejected(new MockMultipartFile("profileImage", "photo.png", "image/png", bytes),
                "INVALID_IMAGE_FORMAT", 400);
    }

    @Test
    void convertsReadFailureToAControlledClientError() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(64L);
        when(file.getBytes()).thenThrow(new IOException("Read failed"));
        assertRejected(file, "IMAGE_READ_FAILED", 400);
    }

    private static void assertRejected(MultipartFile file, String code, int status) {
        BusinessException error = assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(file));
        assertThat(error.getCode()).isEqualTo(code);
        assertThat(error.getStatus().value()).isEqualTo(status);
    }
}
