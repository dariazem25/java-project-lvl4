package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {

    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private static Transaction transaction;
    private static MockWebServer server;
    private static DateTimeFormatter formatter;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
        database.script().run("/seed-test-db.sql");
        server = new MockWebServer();
        server.start();
        formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        server.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Nested
    class RootTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц");
            assertThat(response.getBody()).contains("Бесплатно проверяйте сайты на SEO пригодность");
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testListUrls() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://www.oreilly.com/");
            assertThat(body).contains("https://www.amazon.com/");
        }

        @Test
        void testShowUrl() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/1")
                    .asString();
            String body = response.getBody();

            Url url = new QUrl()
                    .id.equalTo(1)
                    .findOne();

            String id = String.valueOf(url.getId());
            String createdAt = formatter.format(url.getCreatedAt());

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("ID");
            assertThat(body).contains(id);
            assertThat(body).contains("Имя");
            assertThat(body).contains(url.getName());
            assertThat(body).contains("Дата создания");
            assertThat(body).contains(createdAt);
        }

        @Test
        void testCreateValidUrlWithoutPort() {
            String site = "https://hexlet.io";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", site)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(site);

            Url actualUrl = new QUrl()
                    .name.equalTo(site)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(site);
        }

        @Test
        void testCreateValidUrlWithPort() {
            String site = "https://github.com:8081";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", site)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(site);

            Url actualUrl = new QUrl()
                    .name.equalTo(site)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(site);
        }

        @Test
        void testCreateExistingUrl() {
            String name = "https://stackoverflow.com";
            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", name)
                    .asString();

            assertThat(responsePost1.getStatus()).isEqualTo(302);
            assertThat(responsePost1.getHeaders().getFirst("Location")).isEqualTo("/urls");

            List<Url> urlsBefore = new QUrl()
                    .name.equalTo(name)
                    .findList();

            assertThat(urlsBefore.size()).isEqualTo(1);

            HttpResponse<String> responsePost2 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", name)
                    .asString();

            assertThat(responsePost2.getStatus()).isEqualTo(302);
            assertThat(responsePost2.getHeaders().getFirst("Location")).isEqualTo("/urls");

            List<Url> urlsAfter = new QUrl()
                    .name.equalTo(name)
                    .findList();

            assertThat(urlsAfter.size()).isEqualTo(1);
            assertThat(urlsAfter.get(0).getName()).isEqualTo(name);
        }

        @Test
        void testCreateMalformedUrl() {
            String site = "ttps://www.oreilly.com";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", site)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            Url nonexistentUrl = new QUrl()
                    .name.equalTo(site)
                    .findOne();

            assertThat(nonexistentUrl).isNull();
        }

        @Test
        void testUrlCheck() throws IOException {
            // create valid Url
            HttpUrl mockUrl = server.url("/nonexistentsite.com");
            String validUrl = mockUrl.scheme() + "://" + mockUrl.host() + ":" + mockUrl.port();

            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", validUrl)
                    .asString();

            assertThat(responsePost1.getStatus()).isEqualTo(302);
            assertThat(responsePost1.getHeaders().getFirst("Location")).isEqualTo("/urls");

            Url url = new QUrl()
                    .name.equalTo(validUrl)
                    .findOne();

            // check url
            String body = Files.readString(Path.of("src/test/resources/Site.html"));
            server.enqueue(new MockResponse().setBody(body));

            HttpResponse<String> responsePost2 = Unirest
                    .post(baseUrl + "/urls/" + url.getId() + "/checks")
                    .asString();

            assertThat(responsePost2.getStatus()).isEqualTo(302);
            assertThat(responsePost2.getHeaders().getFirst("Location")).isEqualTo("/urls/" + url.getId());

            UrlCheck lastCheck = url.getUrlChecks().get(url.getUrlChecks().size() - 1);

            String createdAt = formatter.format(lastCheck.getCreatedAt());
            String id = String.valueOf(lastCheck.getId());
            String statusCode = String.valueOf(lastCheck.getStatusCode());

            // check is shown on the page /urls/{id}
            HttpResponse<String> response3 = Unirest
                    .get(baseUrl + "/urls/" + url.getId())
                    .asString();
            String body1 = response3.getBody();

            assertThat(response3.getStatus()).isEqualTo(200);
            assertThat(body1).contains(id);
            assertThat(body1).contains("Код ответа");
            assertThat(body1).contains(statusCode);
            assertThat(body1).contains("title");
            assertThat(body1).contains(lastCheck.getTitle());
            assertThat(body1).contains("h1");
            assertThat(body1).contains(lastCheck.getH1());
            assertThat(body1).contains("description");
            assertThat(body1).contains(lastCheck.getDescription());
            assertThat(body1).contains("Дата проверки");
            assertThat(body1).contains(createdAt);

            // urls table is updated with the last check for url
            HttpResponse<String> response4 = Unirest
                    .get(baseUrl + "/urls/")
                    .asString();
            String body2 = response4.getBody();

            assertThat(body2).contains("ID");
            assertThat(body2).contains(String.valueOf(url.getId()));
            assertThat(body2).contains("Имя");
            assertThat(body2).contains(url.getName());
            assertThat(body2).contains("Последняя проверка");
            assertThat(body2).contains(createdAt);
            assertThat(body2).contains("Код ответа");
            assertThat(body2).contains(statusCode);
        }
    }
}
