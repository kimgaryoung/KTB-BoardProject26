package com.example.boardproject;

import com.example.boardproject.auth.JwtProvider;
import com.example.boardproject.entity.*;
import com.example.boardproject.repository.*;
import com.example.boardproject.service.SiteBoardService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:minecraft-site;MODE=MySQL;DATABASE_TO_UPPER=FALSE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.hikari.maximum-pool-size=4",
        "spring.datasource.hikari.connection-timeout=1500"
})
class MinecraftSiteFlowTest {
    @LocalServerPort int port;
    @Autowired CommunityRepository communities;
    @Autowired SiteBoardService siteBoardService;
    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired PostRepository posts;
    @Autowired PostProfileRepository postProfiles;
    @Autowired JwtProvider jwt;
    final HttpClient client = HttpClient.newHttpClient();
    String token;
    long userId;

    @BeforeEach
    void user() {
        var user = users.save(new User(UUID.randomUUID()+"@example.test", "test-password"));
        userId = user.getUserId();
        profiles.save(new UserProfile(userId,"마크테스터",null));
        token = jwt.createAccessToken(userId);
    }

    HttpResponse<String> request(String method, String path, String json, boolean login) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/v1"+path));
        if (login) builder.header("Authorization", "Bearer "+token);
        if (json != null) builder.header("Content-Type","application/json");
        return client.send(builder.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    long board() throws Exception {
        var response = request("GET","/site/board",null,false);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(),"$.data.name")).isEqualTo("마인크래프트");
        return JsonPath.<Number>read(response.body(),"$.data.id").longValue();
    }

    @Test void canonicalBoardIsPublicAndStable() throws Exception {
        long id = board();
        assertThat(board()).isEqualTo(id);
        assertThat(communities.findByNameKey("마인크래프트")).isPresent();
    }

    @Test void concurrentBootstrapReturnsOneBoard() throws Exception {
        communities.findByNameKey("마인크래프트").ifPresent(communities::delete);
        communities.flush();
        var executor = Executors.newFixedThreadPool(6);
        try {
            var futures = IntStream.range(0,6).mapToObj(i -> executor.submit(() -> {
                siteBoardService.initializeBoard();
                return siteBoardService.getBoard().id();
            })).toList();
            long expected = futures.get(0).get();
            for (var future : futures) assertThat(future.get()).isEqualTo(expected);
            assertThat(communities.findAll().stream().filter(c -> c.getNameKey().equals("마인크래프트")).count()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    @Test void concurrentPostReadsDoNotExhaustTheConnectionPool() throws Exception {
        board();
        var executor = Executors.newFixedThreadPool(24);
        var start = new CountDownLatch(1);
        try {
            var futures = IntStream.range(0,24).mapToObj(i -> executor.submit(() -> {
                start.await();
                return request("GET","/posts",null,false).statusCode();
            })).toList();
            start.countDown();
            for (var future : futures) assertThat(future.get(10,TimeUnit.SECONDS)).isEqualTo(200);
        } finally { executor.shutdownNow(); }
    }

    @Test void communityManagementIsRemoved() throws Exception {
        long count = communities.count();
        assertThat(request("GET","/communities",null,false).statusCode()).isEqualTo(410);
        assertThat(request("POST","/communities","{\"name\":\"주술회전\"}",true).statusCode()).isEqualTo(410);
        assertThat(communities.count()).isEqualTo(count);
    }

    @Test void postsDefaultToMinecraftAndOtherBoardsCannotLeakOrAcceptWrites() throws Exception {
        long minecraft = board();
        var other = communities.saveAndFlush(new Community("다른 작품 "+UUID.randomUUID(),"other-"+UUID.randomUUID(),"보존 데이터",userId));
        var archived = posts.saveAndFlush(new Post(userId,"숨겨진 예전 글",null,LocalDateTime.now(),"타 게시판",other.getId()));
        postProfiles.saveAndFlush(new PostProfile(archived));
        var created = request("POST","/posts","{\"title\":\"마크 전용 글\",\"content\":\"건축 이야기\"}",true);
        assertThat(created.statusCode()).isEqualTo(201);
        long postId = JsonPath.<Number>read(created.body(),"$.data.id").longValue();
        assertThat(posts.findById(postId).orElseThrow().getCommunityId()).isEqualTo(minecraft);
        assertThat(request("GET","/posts",null,false).body()).contains("마크 전용 글").doesNotContain("숨겨진 예전 글");
        assertThat(request("GET","/posts/search?keyword=글",null,false).body()).doesNotContain("숨겨진 예전 글");
        assertThat(request("GET","/posts?communityId="+other.getId(),null,false).statusCode()).isEqualTo(404);
        assertThat(request("GET","/posts/"+archived.getPostId(),null,false).statusCode()).isEqualTo(404);
        assertThat(request("POST","/posts","{\"title\":\"잘못된 소속\",\"content\":\"내용\",\"communityId\":"+other.getId()+"}",true).statusCode()).isEqualTo(404);
        assertThat(request("POST","/posts/"+archived.getPostId()+"/likes",null,true).statusCode()).isEqualTo(404);
        assertThat(request("POST","/posts/"+archived.getPostId()+"/comments","{\"commentContent\":\"댓글\"}",true).statusCode()).isEqualTo(404);
        assertThat(posts.existsById(archived.getPostId())).isTrue();
    }

    @Test void writingStillRequiresLogin() throws Exception {
        assertThat(request("POST","/posts","{\"title\":\"글\",\"content\":\"내용\"}",false).statusCode()).isEqualTo(401);
    }
}
