package com.backend.integratedworker.collectingjob.service.crawler.toss;

import com.backend.commondataaccess.exception.CrawlingException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

/**
 * TossPostParser 는 발행일을 얻기 위해 아티클 URL 로 HTTP GET 을 보낸다.
 * 실제 네트워크 대신 loopback 에 띄운 JDK HttpServer 가 아티클 HTML 을 응답한다.
 */
@DisplayName("TossPostParser 테스트")
@ExtendWith(MockitoExtension.class)
class TossPostParserTest {

    private static final String ARTICLE_HTML = """
            <html><head><title>토스 아티클</title></head>
            <body><script>self.__next_f.push([1,"{\\"publishedTime\\":\\"2025-03-10T09:00:00+09:00\\",\\"title\\":\\"t\\"}"])</script></body>
            </html>
            """;

    private static final String ARTICLE_HTML_WITHOUT_DATE = "<html><body>no date here</body></html>";

    @InjectMocks
    private TossPostParser tossPostParser;

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/tech/article/with-date", exchange -> respond(exchange, ARTICLE_HTML));
        server.createContext("/tech/article/without-date", exchange -> respond(exchange, ARTICLE_HTML_WITHOUT_DATE));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * <a data-log-item_title="{title}" href="{href}">
     *   <div><div>
     *     <div>카테고리</div><div>제목</div><div>{summary}</div>
     *   </div><img src="{src}"></div>
     * </a>
     */
    private WebElement newPostElement(String title, String href) {
        WebElement post = Mockito.mock(WebElement.class);
        Mockito.doReturn(title).when(post).getAttribute("data-log-item_title");
        Mockito.doReturn(href).when(post).getAttribute("href");
        return post;
    }

    private WebElement newTextElement(String text) {
        WebElement element = Mockito.mock(WebElement.class);
        Mockito.doReturn(text).when(element).getText();
        return element;
    }

    @DisplayName("TossPost 파싱 테스트")
    @Nested
    class ParseTest {

        @Test
        void 제목_요약_썸네일과_아티클의_발행일을_파싱한다() {
            // given
            WebElement post = newPostElement("토스 아티클", baseUrl + "/tech/article/with-date");
            WebElement contentContainer = Mockito.mock(WebElement.class);
            WebElement summaryDiv = Mockito.mock(WebElement.class);
            Mockito.doReturn("  아티클 요약  ").when(summaryDiv).getText();
            WebElement img = Mockito.mock(WebElement.class);

            Mockito.doReturn(contentContainer).when(post).findElement(By.xpath("./div[1]/div"));
            Mockito.doReturn(List.of(Mockito.mock(WebElement.class), Mockito.mock(WebElement.class), summaryDiv))
                   .when(contentContainer).findElements(By.xpath("./div"));
            Mockito.doReturn(img).when(post).findElement(By.cssSelector("img"));
            Mockito.doReturn("https://static.toss.im/thumb.png").when(img).getAttribute("src");

            // when
            TossPost result = tossPostParser.parse(post);

            // then
            Assertions.assertThat(result.getTitle()).isEqualTo("토스 아티클");
            Assertions.assertThat(result.getUrl()).isEqualTo(baseUrl + "/tech/article/with-date");
            Assertions.assertThat(result.getPublishedAt()).isEqualTo(LocalDate.of(2025, 3, 10));
            Assertions.assertThat(result.getSummary()).contains("아티클 요약");
            Assertions.assertThat(result.getThumbnailUrl()).contains("https://static.toss.im/thumb.png");
        }

        @Test
        void 요약_div가_3개_미만이고_썸네일이_없으면_둘다_empty이다() {
            // given
            WebElement post = newPostElement("토스 아티클", baseUrl + "/tech/article/with-date");
            WebElement contentContainer = Mockito.mock(WebElement.class);

            Mockito.doReturn(contentContainer).when(post).findElement(By.xpath("./div[1]/div"));
            Mockito.doReturn(List.of(Mockito.mock(WebElement.class)))
                   .when(contentContainer).findElements(By.xpath("./div"));
            Mockito.doThrow(new NoSuchElementException("no img")).when(post).findElement(By.cssSelector("img"));

            // when
            TossPost result = tossPostParser.parse(post);

            // then
            Assertions.assertThat(result.getSummary()).isEmpty();
            Assertions.assertThat(result.getThumbnailUrl()).isEmpty();
        }

        @Test
        void 요약_텍스트가_공백이면_empty이다() {
            // given
            WebElement post = newPostElement("토스 아티클", baseUrl + "/tech/article/with-date");
            WebElement contentContainer = Mockito.mock(WebElement.class);

            Mockito.doReturn(contentContainer).when(post).findElement(By.xpath("./div[1]/div"));
            Mockito.doReturn(List.of(Mockito.mock(WebElement.class), Mockito.mock(WebElement.class), newTextElement("   ")))
                   .when(contentContainer).findElements(By.xpath("./div"));
            Mockito.doThrow(new NoSuchElementException("no img")).when(post).findElement(By.cssSelector("img"));

            // when
            TossPost result = tossPostParser.parse(post);

            // then
            Assertions.assertThat(result.getSummary()).isEmpty();
        }

        @Test
        void 요약_영역이_없으면_empty이다() {
            // given
            WebElement post = newPostElement("토스 아티클", baseUrl + "/tech/article/with-date");
            Mockito.doThrow(new NoSuchElementException("no content")).when(post).findElement(By.xpath("./div[1]/div"));
            Mockito.doThrow(new NoSuchElementException("no img")).when(post).findElement(By.cssSelector("img"));

            // when
            TossPost result = tossPostParser.parse(post);

            // then
            Assertions.assertThat(result.getSummary()).isEmpty();
        }

        @Test
        void 아티클에서_발행일을_찾지_못하면_CrawlingException을_던진다() {
            // given
            WebElement post = newPostElement("토스 아티클", baseUrl + "/tech/article/without-date");
            Mockito.doThrow(new NoSuchElementException("no content")).when(post).findElement(By.xpath("./div[1]/div"));
            Mockito.doThrow(new NoSuchElementException("no img")).when(post).findElement(By.cssSelector("img"));

            // when & then
            Assertions.assertThatThrownBy(() -> tossPostParser.parse(post))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("published date not found");
        }

        @Test
        void 아티클_요청이_실패하면_CrawlingException을_던진다() {
            // given
            String unreachableUrl = baseUrl + "/tech/article/with-date";
            server.stop(0);
            WebElement post = newPostElement("토스 아티클", unreachableUrl);
            Mockito.doThrow(new NoSuchElementException("no content")).when(post).findElement(By.xpath("./div[1]/div"));
            Mockito.doThrow(new NoSuchElementException("no img")).when(post).findElement(By.cssSelector("img"));

            // when & then
            Assertions.assertThatThrownBy(() -> tossPostParser.parse(post))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("Failed to fetch Toss article published date");
        }

        @Test
        void WebElement가_아니면_CrawlingException을_던진다() {
            Assertions.assertThatThrownBy(() -> tossPostParser.parse(null))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("expects WebElement");
        }

        @Test
        void 제목_속성이_없으면_CrawlingException을_던진다() {
            // given
            WebElement post = Mockito.mock(WebElement.class);
            Mockito.doReturn(null).when(post).getAttribute("data-log-item_title");

            // when & then
            Assertions.assertThatThrownBy(() -> tossPostParser.parse(post))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("title attribute not found");
        }

        @Test
        void href_속성이_없으면_CrawlingException을_던진다() {
            // given
            WebElement post = newPostElement("토스 아티클", null);

            // when & then
            Assertions.assertThatThrownBy(() -> tossPostParser.parse(post))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("href attribute not found");
        }
    }

    @DisplayName("발행일 파싱 테스트")
    @Nested
    class ParsePublishedAtTest {

        @Test
        void ISO_오프셋_날짜시간을_LocalDate로_변환한다() {
            Assertions.assertThat(tossPostParser.parsePublishedAt("2025-03-10T23:30:00+09:00"))
                      .isEqualTo(LocalDate.of(2025, 3, 10));
        }

        @Test
        void 포맷이_다르면_CrawlingException을_던진다() {
            Assertions.assertThatThrownBy(() -> tossPostParser.parsePublishedAt("2025.03.10"))
                      .isInstanceOf(CrawlingException.class);
        }
    }
}
