package com.example.boardproject.service;

import com.example.boardproject.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "app.images.storage", havingValue = "local")
public class LocalImageStorage implements PostImageStorage {
    private static final String PREFIX = "local/posts/";
    private static final Pattern FILE_NAME = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|gif|webp|bmp)");
    private final Path directory;
    private final String contextPath;
    private final String s3BaseUrl;

    public LocalImageStorage(@Value("${app.images.local-directory:./uploads}") String directory,
            @Value("${server.servlet.context-path:}") String contextPath,
            @Value("${cloud.aws.s3.base-url:}") String s3BaseUrl) {
        this.directory = Path.of(directory).toAbsolutePath().normalize().resolve("posts");
        this.contextPath = contextPath.replaceAll("/+$", "");
        this.s3BaseUrl = s3BaseUrl.replaceAll("/+$", "");
    }

    @Override
    /*
     * 기능 : 개발 환경의 로컬 사진 저장
     * 설명 : 검증한 사진을 UUID 파일명으로 지정된 posts 폴더에 새로 저장하고 DB에 보관할 로컬 키를 반환합니다.
     */
    public String upload(MultipartFile file) {
        var image = ImageUploadValidator.validate(file);
        String name = UUID.randomUUID() + "." + image.extension();
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(name), image.bytes(), StandardOpenOption.CREATE_NEW);
            return PREFIX + name;
        } catch (IOException exception) { throw new BusinessException("IMAGE_UPLOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR); }
    }

    @Override
    /*
     * 기능 : 저장 키를 사진 조회 주소로 변환
     * 설명 : 로컬 키는 이미지 제공 API 주소로 바꿉니다. 기존 S3 키는 설정된 공개 기본 주소가 있을 때 해당 주소로 변환합니다.
     */
    public String getImageUrl(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.startsWith(PREFIX) && FILE_NAME.matcher(key.substring(PREFIX.length())).matches()) {
            return contextPath + "/images/posts/" + key.substring(PREFIX.length());
        }
        return !key.startsWith("local/") && !s3BaseUrl.isBlank() ? s3BaseUrl + "/" + key.replaceFirst("^/+", "") : null;
    }

    /*
     * 기능 : 로컬 사진 파일 읽기
     * 설명 : UUID 형식의 허용 파일명과 일반 파일 여부를 검사해 경로 이탈이나 심볼릭 링크 접근을 막습니다.
     */
    public Resource load(String name) {
        if (!FILE_NAME.matcher(name).matches()) throw notFound();
        Path file = directory.resolve(name);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw notFound();
        return new FileSystemResource(file);
    }
    private BusinessException notFound() { return new BusinessException("IMAGE_NOT_FOUND", HttpStatus.NOT_FOUND); }
}
