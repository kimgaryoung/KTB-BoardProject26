package com.example.boardproject;

import com.example.boardproject.exception.BusinessException;
import com.example.boardproject.service.S3ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class S3ImageServiceTest {
    private final S3Client client = mock(S3Client.class);
    private final S3ImageService images = new S3ImageService(client);

    @Test
    void s3UploadUsesDetectedImageTypeAndExposesItsPublicUrl() throws Exception {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
        ReflectionTestUtils.setField(images, "bucket", "test-bucket");
        ReflectionTestUtils.setField(images, "baseUrl", "https://images.example.test/");
        AtomicReference<PutObjectRequest> uploaded = new AtomicReference<>();
        AtomicReference<byte[]> bytes = new AtomicReference<>();
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(call -> {
            uploaded.set(call.getArgument(0));
            try (var input = ((RequestBody) call.getArgument(1)).contentStreamProvider().newStream()) { bytes.set(input.readAllBytes()); }
            return PutObjectResponse.builder().build();
        });
        String key = images.upload(new MockMultipartFile("image", "untrusted.html", "application/octet-stream", png));
        assertThat(key).startsWith("posts/").endsWith(".png");
        assertThat(uploaded.get().bucket()).isEqualTo("test-bucket");
        assertThat(uploaded.get().key()).isEqualTo(key);
        assertThat(uploaded.get().contentType()).isEqualTo("image/png");
        assertThat(bytes.get()).isEqualTo(png);
        assertThat(images.getImageUrl(key)).isEqualTo("https://images.example.test/" + key);
    }

    @Test
    void invalidContentNeverReachesS3() {
        assertThatThrownBy(() -> images.upload(new MockMultipartFile("image", "photo.png", "image/png", "not an image".getBytes())))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(client);
    }
}
