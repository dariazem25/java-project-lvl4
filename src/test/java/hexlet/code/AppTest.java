package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {

    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private static Transaction transaction;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed-test-db.sql");
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

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("ID");
            assertThat(body).contains("1");
            assertThat(body).contains("Имя");
            assertThat(body).contains("https://www.oreilly.com/");
            assertThat(body).contains("Дата создания");
            assertThat(body).contains("01/01/2022 13:57");
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
        String site = "https://www.oreilly.com";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", site)
                .asString();

        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        List<Url> urls = new QUrl()
                .name.equalTo(site)
                .findList();

        assertThat(urls.size()).isEqualTo(1);
        assertThat(urls.get(0).getName()).isEqualTo(site);
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
}
