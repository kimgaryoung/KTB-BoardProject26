package com.example.boardproject;

import com.example.boardproject.auth.JwtProvider;
import com.example.boardproject.entity.User;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.repository.UserRepository;
import com.example.boardproject.repository.UserProfileRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.site.community-management-enabled=true",
        "app.images.storage=local", "spring.datasource.url=jdbc:h2:mem:images-test;MODE=MySQL;DATABASE_TO_UPPER=FALSE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1"
})
class PostImageFlowTest {
    private static final Path DIRECTORY;
    static {
        try { DIRECTORY = Files.createTempDirectory("ktb-post-images-test-"); }
        catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }
    private static final byte[] PNG = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) { registry.add("app.images.local-directory", DIRECTORY::toString); }
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired JwtProvider jwt;
    @Autowired com.example.boardproject.auth.JwtProperties jwtProperties;
    private final HttpClient client = HttpClient.newHttpClient();
    private String token;

    @BeforeEach
    void login() {
        User user = users.save(new User(UUID.randomUUID() + "@example.test", "test-password"));
        profiles.save(new UserProfile(user.getUserId(), "사진테스터", null));
        token = jwt.createAccessToken(user.getUserId());
    }

    @AfterAll
    static void cleanup() throws Exception {
        try (var files = Files.walk(DIRECTORY)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
        }
    }

    @Test
    void uploadedPhotoIsPublicAndAppearsInBothListAndSearchThumbnails() throws Exception {
        long id = createImagePost();
        var detail = get("/posts/" + id);
        String url = JsonPath.read(detail.body(), "$.data.postImageUrl");
        assertThat(url).startsWith("/v1/images/posts/").endsWith(".png");
        var image = client.send(HttpRequest.newBuilder(URI.create(base() + url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(image.statusCode()).isEqualTo(200);
        assertThat(image.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(image.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(image.body()).isEqualTo(PNG);
        List<String> listUrls = JsonPath.read(get("/posts?communityId=0").body(), "$.data[?(@.id == " + id + ")].thumbnailUrl");
        assertThat(listUrls).containsExactly(url);
        List<String> searchUrls = JsonPath.read(get("/posts/search?communityId=0&keyword=photo").body(), "$.data[*].thumbnailUrl");
        assertThat(searchUrls).contains(url);
    }

    @Test
    void editingTextKeepsPhotoAndMultipartCanReplaceOrRemoveIt() throws Exception {
        long id = createImagePost();
        String original = JsonPath.read(get("/posts/" + id).body(), "$.data.postImageUrl");
        var textEdit = json("PATCH", "/posts/" + id, "{\"title\":\"photo edit\",\"content\":\"new text\"}");
        assertThat(textEdit.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(get("/posts/" + id).body(), "$.data.postImageUrl")).isEqualTo(original);
        assertThat(multipart("PATCH", "/posts/" + id, "{\"title\":\"photo edit\",\"content\":\"new text\"}", PNG, "image/png", "renamed.txt").statusCode()).isEqualTo(200);
        String replaced = JsonPath.read(get("/posts/" + id).body(), "$.data.postImageUrl");
        assertThat(replaced).isNotEqualTo(original).endsWith(".png");
        assertThat(json("PATCH", "/posts/" + id, "{\"title\":\"photo removed\",\"content\":\"new text\",\"removeImage\":true}").statusCode()).isEqualTo(200);
        assertThat((Object) JsonPath.read(get("/posts/" + id).body(), "$.data.postImageUrl")).isNull();
    }

    @Test
    void invalidPhotosAndContradictoryUpdatesAreRejectedWithoutReplacingOldPhoto() throws Exception {
        long id = createImagePost();
        String original = JsonPath.read(get("/posts/" + id).body(), "$.data.postImageUrl");
        String request = "{\"title\":\"photo\",\"content\":\"text\",\"communityId\":0}";
        for (byte[] invalid : List.of(new byte[0], "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))) {
            assertThat(multipart("POST", "/posts", request, invalid, "image/png", "photo.png").statusCode()).isEqualTo(400);
        }
        assertThat(multipart("POST", "/posts", request, "<svg xmlns='http://www.w3.org/2000/svg'></svg>".getBytes(), "image/svg+xml", "photo.svg").statusCode()).isEqualTo(400);
        assertThat(multipart("PATCH", "/posts/" + id, "{\"title\":\"photo\",\"content\":\"text\",\"removeImage\":true}", PNG, "image/png", "photo.png").statusCode()).isEqualTo(400);
        byte[] large = Arrays.copyOf(PNG, 5 * 1024 * 1024 + 1);
        assertThat(multipart("POST", "/posts", request, large, "image/png", "photo.png").statusCode()).isIn(400, 413);
        assertThat((String) JsonPath.read(get("/posts/" + id).body(), "$.data.postImageUrl")).isEqualTo(original);
    }

    @Test
    void onlyAuthorCanChangePhotosAndMissingImageFilesAreNotPublicDocuments() throws Exception {
        long id = createImagePost();
        long before;
        try (var files = Files.walk(DIRECTORY)) { before = files.filter(Files::isRegularFile).count(); }
        login();
        assertThat(multipart("PATCH", "/posts/" + id, "{\"title\":\"photo\",\"content\":\"text\"}", PNG, "image/png", "photo.png").statusCode()).isEqualTo(403);
        try (var files = Files.walk(DIRECTORY)) { assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(before); }
        assertThat(get("/images/posts/not-a-generated-photo.png").statusCode()).isEqualTo(404);
        assertThat(get("/images/posts/00000000-0000-0000-0000-000000000000.png").statusCode()).isEqualTo(404);
        assertThat(get("/images/posts/application.yml").statusCode()).isEqualTo(404);
    }

    @Test
    void anonymousAndExpiredSessionsCannotUploadOrCreatePosts() throws Exception {
        String request = "{\"title\":\"photo\",\"content\":\"text\",\"communityId\":0}";
        token = null;
        assertThat(multipart("POST", "/posts", request, PNG, "image/png", "photo.png").statusCode()).isEqualTo(401);
        token = io.jsonwebtoken.Jwts.builder().subject("1").claim("typ", "access")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThat(multipart("POST", "/posts", request, PNG, "image/png", "photo.png").statusCode()).isEqualTo(401);
        assertThat(json("POST", "/posts", request).statusCode()).isEqualTo(401);
    }

    private long createImagePost() throws Exception {
        var created = multipart("POST", "/posts", "{\"title\":\"photo\",\"content\":\"image in body\",\"communityId\":0}", PNG, "image/png", "photo.png");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        return ((Number) JsonPath.read(created.body(), "$.data.id")).longValue();
    }

    private HttpResponse<String> multipart(String method, String path, String json, byte[] image, String type, String filename) throws Exception {
        String boundary = "test-" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"request\"\r\nContent-Type: application/json\r\n\r\n" + json + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"" + filename + "\"\r\nContent-Type: " + type + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(image);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var builder = HttpRequest.newBuilder(URI.create(base() + "/v1" + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.header("Content-Type", "multipart/form-data; boundary=" + boundary).method(method, HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    }
    private String base() { return "http://localhost:" + port; }
    private HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create(base() + "/v1" + path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> json(String method, String path, String body) throws Exception { return client.send(HttpRequest.newBuilder(URI.create(base() + "/v1" + path)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()); }
}
