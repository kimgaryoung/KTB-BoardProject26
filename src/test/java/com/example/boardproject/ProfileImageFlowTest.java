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
        "app.images.storage=local",
        "spring.datasource.url=jdbc:h2:mem:profile-images;MODE=MySQL;DATABASE_TO_UPPER=FALSE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1"
})
class ProfileImageFlowTest {
    private static final Path DIRECTORY;
    static {
        try { DIRECTORY = Files.createTempDirectory("ktb-profile-images-test-"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private static final byte[] PNG = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/nKkAAAAASUVORK5CYII=");
    @DynamicPropertySource static void storage(DynamicPropertyRegistry properties) { properties.add("app.images.local-directory", DIRECTORY::toString); }
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired JwtProvider jwt;
    final HttpClient client = HttpClient.newHttpClient();

    @Test void uploadedPhotoCanBeSavedReadReplacedAndRemoved() throws Exception {
        var user = users.save(new User(UUID.randomUUID()+"@example.test","Password1!"));
        profiles.save(new UserProfile(user.getUserId(),"사진테스터",null));
        String token = jwt.createAccessToken(user.getUserId());
        String first = uploadUrl();
        var image = client.send(HttpRequest.newBuilder(URI.create(base()+first)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(image.statusCode()).isEqualTo(200);
        assertThat(image.body()).isEqualTo(PNG);
        assertThat(image.headers().firstValue("Content-Type").orElse("")).isEqualTo("image/png");
        for (String url : List.of(first,uploadUrl())) {
            assertThat(json("PUT","/users/me","{\"nickname\":\"사진테스터\",\"profileImageUrl\":\""+url+"\"}",token).statusCode()).isEqualTo(200);
            assertThat(JsonPath.<String>read(json("GET","/auth/check",null,token).body(),"$.data.profileImageUrl")).isEqualTo(url);
            assertThat(profiles.findByUserId(user.getUserId()).getProfileImage()).isEqualTo(url);
        }
        assertThat(json("PUT","/users/me","{\"nickname\":\"사진테스터\",\"profileImageUrl\":null}",token).statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Object>read(json("GET","/auth/check",null,token).body(),"$.data.profileImageUrl")).isNull();
    }

    @Test void signupCanAttachPhotoBeforeLoginButUpdatingAnAccountRequiresLogin() throws Exception {
        String url=uploadUrl();
        String email=UUID.randomUUID()+"@example.test";
        assertThat(json("POST","/auth/signup","{\"email\":\""+email+"\",\"password\":\"Password1!\",\"nickname\":\"가입사진\",\"profileImageUrl\":\""+url+"\"}",null).statusCode()).isEqualTo(201);
        var user=users.findByEmail(email).orElseThrow();
        assertThat(profiles.findByUserId(user.getUserId()).getProfileImage()).isEqualTo(url);
        assertThat(json("PUT","/users/me","{\"nickname\":\"변경\",\"profileImageUrl\":null}",null).statusCode()).isEqualTo(401);
    }

    @Test void invalidEmptyAndOversizedProfileImagesAreRejected() throws Exception {
        assertThat(upload(new byte[0]).statusCode()).isEqualTo(400);
        assertThat(upload("<svg onload='alert(1)'/>".getBytes(StandardCharsets.UTF_8)).statusCode()).isEqualTo(400);
        assertThat(upload(Arrays.copyOf(PNG,5*1024*1024+1)).statusCode()).isEqualTo(413);
    }

    private String uploadUrl() throws Exception {
        var result=upload(PNG);
        assertThat(result.statusCode()).as(result.body()).isEqualTo(201);
        return JsonPath.read(result.body(),"$.data.profileImageUrl");
    }
    private HttpResponse<String> upload(byte[] bytes) throws Exception {
        String boundary="profile-"+UUID.randomUUID();
        var body=new ByteArrayOutputStream();
        body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"profileImage\"; filename=\"avatar.txt\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(URI.create(base()+"/v1/users/upload/profile-image")).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    private String base(){return "http://localhost:"+port;}
    private HttpResponse<String> json(String method,String path,String body,String token) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create(base()+"/v1"+path));
        if(token!=null)builder.header("Authorization","Bearer "+token);
        if(body!=null)builder.header("Content-Type","application/json");
        return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
