package com.example.boardproject;

import com.example.boardproject.auth.JwtProvider;
import com.example.boardproject.entity.Post;
import com.example.boardproject.entity.PostProfile;
import com.example.boardproject.entity.User;
import com.example.boardproject.entity.UserProfile;
import com.example.boardproject.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.site.community-management-enabled=true")
class CommunityFlowTest {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired PostRepository posts;
    @Autowired PostProfileRepository postProfiles;
    @Autowired JwtProvider jwt;
    private final HttpClient client = HttpClient.newHttpClient();
    private String token;
    private Long userId;
    private String suffix;

    @BeforeEach
    void login() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = users.save(new User(suffix + "@example.test", "test-password"));
        userId = user.getUserId();
        profiles.save(new UserProfile(userId, "테스터", null));
        token = jwt.createAccessToken(userId);
    }

    @Test
    void discoverCreateAndSwitchCommunitiesWithoutMixingPosts() throws Exception {
        long first = createCommunity("주술회전 " + suffix);
        long second = createCommunity("하이큐 " + suffix);
        long firstPost = createPost(first, "공통제목", "첫 커뮤니티");
        long secondPost = createPost(second, "공통제목", "둘째 커뮤니티");
        createPost(second, "다른제목", "공통제목이 내용에만 포함");

        var discovery = request("GET", "/communities?keyword=" + encode("주술회전 " + suffix), null, false);
        assertThat(discovery.statusCode()).isEqualTo(200);
        assertThat(ids(discovery)).containsExactly(first);
        assertThat(ids(request("GET", "/posts?communityId=" + first, null, false))).containsExactly(firstPost);
        assertThat(ids(request("GET", "/posts?communityId=" + second + "&cursor=" + secondPost, null, false))).isEmpty();
        assertThat(ids(request("GET", "/posts/search?communityId=" + first + "&keyword=" + encode("공통제목"), null, false)))
                .containsExactly(firstPost);
        var detail = request("GET", "/posts/" + firstPost, null, false);
        assertThat(number(detail, "$.data.communityId")).isEqualTo(first);
        var update = request("PATCH", "/posts/" + firstPost,
                "{\"title\":\"수정\",\"content\":\"내용\",\"communityId\":" + second + "}", true);
        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(number(request("GET", "/posts/" + firstPost, null, false), "$.data.communityId")).isEqualTo(first);
    }

    @Test
    void communityNamesAreNormalizedAndDuplicatesReturnConflict() throws Exception {
        long id = createCommunity("  하이큐　 " + suffix + "  ");
        var detail = request("GET", "/communities/" + id, null, false);
        assertThat((String) JsonPath.read(detail.body(), "$.data.name")).isEqualTo("하이큐 " + suffix);
        var duplicate = request("POST", "/communities",
                "{\"name\":\"하이큐" + suffix.toUpperCase() + "\"}", true);
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat((String) JsonPath.read(duplicate.body(), "$.code")).isEqualTo("COMMUNITY_ALREADY_EXISTS");
        assertThat(number(duplicate, "$.communityId")).isEqualTo(id);
        assertThat(request("POST", "/communities", "{\"name\":\"   \"}", true).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/communities", "{\"name\":\"" + "가".repeat(51) + "\"}", true).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/communities", "{\"name\":\"소개검증" + suffix + "\",\"description\":\"" + "가".repeat(301) + "\"}", true).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/communities", "{\"name\":\"미인증\"}", false).statusCode()).isEqualTo(401);
        assertThat(request("GET", "/communities?offset=-1", null, false).statusCode()).isEqualTo(400);
        assertThat(ids(request("GET", "/communities?keyword=%25", null, false))).isEmpty();
        assertThat(number(request("GET", "/communities?limit=1", null, false), "$.data[0].id")).isZero();
        assertThat(ids(request("GET", "/communities?offset=1&limit=1", null, false))).doesNotContain(0L);
    }

    @Test
    void simultaneousCommunityCreationCreatesOnlyOneCommunity() throws Exception {
        var requests = java.util.stream.IntStream.range(0, 8).mapToObj(i ->
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/communities"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"동시생성 " + suffix + "\"}"))
                        .build()).toList();
        var futures = requests.stream().map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString())).toList();
        var responses = futures.stream().map(java.util.concurrent.CompletableFuture::join).toList();
        assertThat(responses.stream().map(HttpResponse::statusCode).toList()).containsOnly(201, 409);
        assertThat(responses.stream().filter(response -> response.statusCode() == 201).count()).isEqualTo(1);
        var created = responses.stream().filter(response -> response.statusCode() == 201).findFirst().orElseThrow();
        long createdId = number(created, "$.data.id");
        responses.stream().filter(response -> response.statusCode() == 409).forEach(response ->
                assertThat(number(response, "$.communityId")).isEqualTo(createdId));
        assertThat(ids(request("GET", "/communities?keyword=" + encode("동시생성 " + suffix), null, false))).containsExactly(createdId);
    }

    @Test
    void legacyPostsStayInFreeBoardAndUnknownCommunityIsRejected() throws Exception {
        Post legacy = posts.save(new Post(userId, "기존글", null, LocalDateTime.now(), "기존내용"));
        postProfiles.save(new PostProfile(legacy));
        var list = request("GET", "/posts?communityId=0", null, false);
        assertThat(ids(list)).contains(legacy.getPostId());
        assertThat(number(request("GET", "/posts/" + legacy.getPostId(), null, false), "$.data.communityId")).isZero();
        assertThat(request("GET", "/communities/0", null, false).statusCode()).isEqualTo(200);
        assertThat(request("GET", "/communities/99999999", null, false).statusCode()).isEqualTo(404);
        assertThat(request("GET", "/posts?communityId=99999999", null, false).statusCode()).isEqualTo(404);
        assertThat(request("POST", "/posts", "{\"title\":\"글\",\"content\":\"내용\"}", true).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/posts", "{\"title\":\"글\",\"content\":\"내용\",\"communityId\":99999999}", true).statusCode()).isEqualTo(404);
        assertThat(createPost(0, "자유글", "내용")).isPositive();
    }

    @Test
    void hiddenCommunitiesAreExcludedBeforePaginationAndCanBeRestored() throws Exception {
        String key = suffix;
        long first = createCommunity("마인크래프트 " + key);
        long second = createCommunity("주술회전 " + key);
        String search = "/communities?keyword=" + key;
        assertThat(request("PUT", "/communities/" + second + "/hidden", null, true).statusCode()).isEqualTo(200);
        assertThat(request("PUT", "/communities/" + second + "/hidden", null, true).statusCode()).isEqualTo(200);
        assertThat(ids(request("GET", search + "&limit=1", null, true))).containsExactly(first);
        assertThat(ids(request("GET", search + "&offset=1&limit=1", null, true))).isEmpty();
        assertThat(ids(request("GET", search + "&hidden=true", null, true))).containsExactly(second);
        assertThat(ids(request("GET", "/communities?hidden=true&keyword=nomatch", null, true))).isEmpty();
        assertThat(request("GET", "/communities/" + second, null, true).statusCode()).isEqualTo(200);
        assertThat(request("DELETE", "/communities/" + second + "/hidden", null, true).statusCode()).isEqualTo(200);
        assertThat(request("DELETE", "/communities/" + second + "/hidden", null, true).statusCode()).isEqualTo(200);
        assertThat(ids(request("GET", search, null, true))).containsExactly(second, first);
        assertThat(ids(request("GET", search + "&hidden=true", null, true))).isEmpty();
    }

    @Test
    void postCreationRequiresAValidLoginEvenWhenCallingTheApiDirectly() throws Exception {
        long before = posts.count();
        String body = "{\"title\":\"로그인 필요\",\"content\":\"비로그인 글\",\"communityId\":0}";
        assertThat(request("POST", "/posts", body, false).statusCode()).isEqualTo(401);
        String validToken = token;
        token = "invalid-token";
        assertThat(request("POST", "/posts", body, true).statusCode()).isEqualTo(401);
        token = validToken;
        assertThat(posts.count()).isEqualTo(before);
        assertThat(request("GET", "/posts?communityId=0", null, false).statusCode()).isEqualTo(200);
    }

    @Test
    void hidingIsPrivateToAnAccountAndGuestsCannotChangePreferences() throws Exception {
        String key = suffix;
        long community = createCommunity("개인숨김 " + key);
        String originalToken = token;
        assertThat(request("PUT", "/communities/" + community + "/hidden", null, true).statusCode()).isEqualTo(200);
        assertThat(ids(request("GET", "/communities?keyword=" + key, null, false))).containsExactly(community);
        login();
        assertThat(ids(request("GET", "/communities?keyword=" + key, null, true))).containsExactly(community);
        assertThat(ids(request("GET", "/communities?keyword=" + key + "&hidden=true", null, true))).isEmpty();
        assertThat(request("DELETE", "/communities/" + community + "/hidden", null, true).statusCode()).isEqualTo(200);
        token = originalToken;
        assertThat(ids(request("GET", "/communities?keyword=" + key + "&hidden=true", null, true))).containsExactly(community);
        assertThat(request("GET", "/communities?hidden=true", null, false).statusCode()).isEqualTo(401);
        assertThat(request("PUT", "/communities/" + community + "/hidden", null, false).statusCode()).isEqualTo(401);
        assertThat(request("DELETE", "/communities/" + community + "/hidden", null, false).statusCode()).isEqualTo(401);
        assertThat(request("PUT", "/communities/99999999/hidden", null, true).statusCode()).isEqualTo(404);
        assertThat(request("PUT", "/communities/-1/hidden", null, true).statusCode()).isEqualTo(400);
    }

    @Test
    void virtualFreeBoardCanBeHiddenAndRestoredWithoutBreakingOffsets() throws Exception {
        createCommunity("페이지확인 " + suffix);
        assertThat(request("PUT", "/communities/0/hidden", null, true).statusCode()).isEqualTo(200);
        assertThat(ids(request("GET", "/communities?limit=1", null, true))).doesNotContain(0L);
        assertThat(ids(request("GET", "/communities?hidden=true&limit=1", null, true))).containsExactly(0L);
        assertThat(ids(request("GET", "/communities?hidden=true&offset=1&limit=1", null, true))).isEmpty();
        assertThat(request("DELETE", "/communities/0/hidden", null, true).statusCode()).isEqualTo(200);
        assertThat(ids(request("GET", "/communities?limit=1", null, true))).containsExactly(0L);
    }

    private long createCommunity(String name) throws Exception {
        var response = request("POST", "/communities", "{\"name\":\"" + name + "\",\"description\":\"팬들의 공간\"}", true);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return number(response, "$.data.id");
    }

    private long createPost(long communityId, String title, String content) throws Exception {
        var response = request("POST", "/posts", "{\"title\":\"" + title + "\",\"content\":\"" + content + "\",\"communityId\":" + communityId + "}", true);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return number(response, "$.data.id");
    }

    private HttpResponse<String> request(String method, String path, String body, boolean authenticated) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1" + path));
        if (authenticated) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private List<Long> ids(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        List<Number> values = JsonPath.read(response.body(), "$.data[*].id");
        return values.stream().map(Number::longValue).toList();
    }

    private long number(HttpResponse<String> response, String path) {
        return ((Number) JsonPath.read(response.body(), path)).longValue();
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
