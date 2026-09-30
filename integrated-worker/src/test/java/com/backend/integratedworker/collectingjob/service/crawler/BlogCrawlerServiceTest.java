package com.backend.integratedworker.collectingjob.service.crawler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import com.backend.commondataaccess.exception.CrawlingException;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoBlogCrawler;
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoPost;
import com.backend.integratedworker.collectingjob.service.crawler.line.LineBlogCrawler;
import com.backend.integratedworker.collectingjob.service.crawler.line.LinePost;
import com.backend.integratedworker.collectingjob.service.crawler.strategy.CrawlerStrategy;
import com.backend.integratedworker.collectingjob.service.crawler.toss.TossBlogCrawler;
import com.backend.integratedworker.collectingjob.service.crawler.toss.TossPost;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

@DisplayName("BlogCrawlerService 테스트")
@ExtendWith(MockitoExtension.class)
class BlogCrawlerServiceTest {

    @Spy
    @InjectMocks
    private BlogCrawlerService blogCrawlerService;

    @Mock
    private KakaoBlogCrawler kakaoBlogCrawler;

    @Mock
    private LineBlogCrawler lineBlogCrawler;

    @Mock
    private TossBlogCrawler tossBlogCrawler;

    private PostProvider newPostProvider(String name) {
        return PostProvider.builder()
                           .id(UUID.randomUUID())
                           .name(name)
                           .baseUrl("https://test.com")
                           .description("test_description")
                           .isUsed(true)
                           .collectSources(new ArrayList<>())
                           .build();
    }

    private CollectingJob newCollectingJob(PostProvider postProvider, int fromPage, int toPage) {
        CollectSource collectSource = CollectSource.builder()
                                                   .id(UUID.randomUUID())
                                                   .postProvider(postProvider)
                                                   .url("https://test.com/blog/1")
                                                   .collectScheduleType(CollectScheduleType.MANUAL)
                                                   .isUsed(true)
                                                   .build();

        return CollectingJob.builder()
                            .id(UUID.randomUUID())
                            .collectSource(collectSource)
                            .jobStatus(JobStatus.RUNNING)
                            .fromPage(fromPage)
                            .toPage(toPage)
                            .build();
    }

    private KakaoPost newKakaoPost(String url) {
        return KakaoPost.builder()
                        .title("test_title")
                        .url(url)
                        .publishedAt(LocalDate.of(2025, 1, 1))
                        .build();
    }

    @DisplayName("fetch 테스트")
    @Nested
    class FetchTest {

        @Test
        void kakao_provider면_fromPage부터_toPage까지_KakaoBlogCrawler로_크롤링한다() {
            // given
            PostProvider postProvider = newPostProvider("kakao");
            CollectingJob collectingJob = newCollectingJob(postProvider, 1, 2);
            KakaoPost page1Post = newKakaoPost("https://test.com/post/1");
            KakaoPost page2Post = newKakaoPost("https://test.com/post/2");

            Mockito.doReturn(List.of(page1Post)).when(blogCrawlerService).crawl(kakaoBlogCrawler, postProvider, 1);
            Mockito.doReturn(List.of(page2Post)).when(blogCrawlerService).crawl(kakaoBlogCrawler, postProvider, 2);

            // when
            List<Post> result = blogCrawlerService.fetch(collectingJob);

            // then
            Assertions.assertThat(result).containsExactly(page1Post, page2Post);
        }

        @Test
        void line_provider면_LineBlogCrawler로_크롤링한다() {
            // given
            PostProvider postProvider = newPostProvider("line");
            CollectingJob collectingJob = newCollectingJob(postProvider, 1, 1);
            LinePost post = LinePost.builder()
                                    .title("test_title")
                                    .url("https://test.com/post/1")
                                    .publishedAt(LocalDate.of(2025, 1, 1))
                                    .build();

            Mockito.doReturn(List.of(post)).when(blogCrawlerService).crawl(lineBlogCrawler, postProvider, 1);

            // when
            List<Post> result = blogCrawlerService.fetch(collectingJob);

            // then
            Assertions.assertThat(result).containsExactly(post);
        }

        @Test
        void toss_provider면_TossBlogCrawler로_크롤링한다() {
            // given
            PostProvider postProvider = newPostProvider("toss");
            CollectingJob collectingJob = newCollectingJob(postProvider, 3, 3);
            TossPost post = TossPost.builder()
                                    .title("test_title")
                                    .url("https://test.com/post/1")
                                    .publishedAt(LocalDate.of(2025, 1, 1))
                                    .build();

            Mockito.doReturn(List.of(post)).when(blogCrawlerService).crawl(tossBlogCrawler, postProvider, 3);

            // when
            List<Post> result = blogCrawlerService.fetch(collectingJob);

            // then
            Assertions.assertThat(result).containsExactly(post);
        }

        @Test
        void fromPage가_toPage보다_크면_크롤링하지_않고_빈_리스트를_반환한다() {
            // given
            CollectingJob collectingJob = newCollectingJob(newPostProvider("kakao"), 2, 1);

            // when
            List<Post> result = blogCrawlerService.fetch(collectingJob);

            // then
            Assertions.assertThat(result).isEmpty();
            Mockito.verify(blogCrawlerService, Mockito.never()).crawl(any(), any(), anyInt());
        }

        @Test
        void 지원하지_않는_provider면_CrawlingException을_던진다() {
            // given
            CollectingJob collectingJob = newCollectingJob(newPostProvider("naver"), 1, 1);

            // when & then
            Assertions.assertThatThrownBy(() -> blogCrawlerService.fetch(collectingJob))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("Unsupported post provider: naver");
        }
    }

    /**
     * 실제 Chrome 을 띄우지 않도록 ChromeDriver 생성과 WebDriverManager 를 모킹한다.
     */
    @DisplayName("crawl 테스트")
    @Nested
    class CrawlTest {

        private static final String CRAWLING_URL = "https://test.com?page=1";
        private static final By POST_SELECTOR = By.cssSelector("ul.list_post li");

        private MockedStatic<WebDriverManager> webDriverManagerStatic;
        private PostProvider postProvider;
        private final List<Path> userDataDirs = new ArrayList<>();

        @Mock
        private CrawlerStrategy<KakaoPost> strategy;

        @BeforeEach
        void setUp() {
            WebDriverManager webDriverManager = Mockito.mock(WebDriverManager.class);
            webDriverManagerStatic = Mockito.mockStatic(WebDriverManager.class);
            webDriverManagerStatic.when(WebDriverManager::chromedriver).thenReturn(webDriverManager);
            postProvider = newPostProvider("kakao");
        }

        @AfterEach
        void tearDown() {
            webDriverManagerStatic.close();
        }

        @SuppressWarnings("unchecked")
        private Path userDataDirOf(ChromeOptions options) {
            Map<String, Object> chromeOptions = (Map<String, Object>) options.asMap().get(ChromeOptions.CAPABILITY);
            List<String> args = (List<String>) chromeOptions.get("args");
            return args.stream()
                       .filter(arg -> arg.startsWith("--user-data-dir="))
                       .map(arg -> Path.of(arg.substring("--user-data-dir=".length())))
                       .findFirst()
                       .orElseThrow();
        }

        private MockedConstruction<ChromeDriver> mockChromeDriver(List<WebElement> elements, RuntimeException getFailure) {
            return mockChromeDriver(elements, getFailure, false);
        }

        private MockedConstruction<ChromeDriver> mockChromeDriver(List<WebElement> elements,
                                                                  RuntimeException getFailure,
                                                                  boolean interruptAfterConstruct) {
            return Mockito.mockConstruction(ChromeDriver.class, (driver, context) -> {
                Path userDataDir = userDataDirOf((ChromeOptions) context.arguments().get(0));
                userDataDirs.add(userDataDir);
                try {
                    // Chrome 이 프로필 파일을 남긴 상황을 흉내내 정리 로직을 검증한다.
                    Files.writeString(Files.createDirectories(userDataDir.resolve("Default")).resolve("Preferences"), "{}");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (getFailure != null) {
                    Mockito.doThrow(getFailure).when(driver).get(CRAWLING_URL);
                }
                if (elements != null) {
                    Mockito.doReturn(elements).when(driver).findElements(POST_SELECTOR);
                }
                if (interruptAfterConstruct) {
                    // 파일 I/O 이후, Thread.sleep 직전에 인터럽트가 걸린 상태를 만든다.
                    Thread.currentThread().interrupt();
                }
            });
        }

        @Test
        void 선택자에_매칭된_요소를_파싱해_반환하고_드라이버와_임시_디렉토리를_정리한다() {
            // given
            WebElement element1 = Mockito.mock(WebElement.class);
            WebElement element2 = Mockito.mock(WebElement.class);
            KakaoPost post1 = newKakaoPost("https://test.com/post/1");
            KakaoPost post2 = newKakaoPost("https://test.com/post/2");

            Mockito.doReturn(CRAWLING_URL).when(strategy).getCrawlingUrl(postProvider, 1);
            Mockito.doReturn(POST_SELECTOR).when(strategy).getPostSelector();
            Mockito.doReturn(post1).when(strategy).parsePost(element1);
            Mockito.doReturn(post2).when(strategy).parsePost(element2);

            try (MockedConstruction<ChromeDriver> chromeDrivers = mockChromeDriver(List.of(element1, element2), null)) {
                // when
                List<KakaoPost> result = blogCrawlerService.crawl(strategy, postProvider, 1);

                // then
                Assertions.assertThat(result).containsExactly(post1, post2);
                Assertions.assertThat(chromeDrivers.constructed()).hasSize(1);
                ChromeDriver driver = chromeDrivers.constructed().get(0);
                Mockito.verify(driver).get(CRAWLING_URL);
                Mockito.verify(driver).quit();
            }
            Assertions.assertThat(userDataDirs).hasSize(1);
            Assertions.assertThat(userDataDirs.get(0)).doesNotExist();
        }

        @Test
        void 페이지_이동_중_예외가_발생하면_CrawlingException으로_감싸고_드라이버를_정리한다() {
            // given
            Mockito.doReturn(CRAWLING_URL).when(strategy).getCrawlingUrl(postProvider, 1);

            try (MockedConstruction<ChromeDriver> chromeDrivers =
                         mockChromeDriver(null, new IllegalStateException("chrome crashed"))) {
                // when & then
                Assertions.assertThatThrownBy(() -> blogCrawlerService.crawl(strategy, postProvider, 1))
                          .isInstanceOf(CrawlingException.class)
                          .hasMessageContaining("Crawl failed provider=kakao page=1")
                          .hasCauseInstanceOf(IllegalStateException.class);

                Mockito.verify(chromeDrivers.constructed().get(0)).quit();
            }
            Assertions.assertThat(userDataDirs.get(0)).doesNotExist();
            Mockito.verify(strategy, Mockito.never()).parsePost(any());
        }

        @Test
        void 대기_중_인터럽트되면_CrawlingException을_던지고_인터럽트_상태를_유지한다() {
            // given
            Mockito.doReturn(CRAWLING_URL).when(strategy).getCrawlingUrl(eq(postProvider), eq(1));

            try (MockedConstruction<ChromeDriver> chromeDrivers = mockChromeDriver(null, null, true)) {
                // when & then
                Assertions.assertThatThrownBy(() -> blogCrawlerService.crawl(strategy, postProvider, 1))
                          .isInstanceOf(CrawlingException.class)
                          .hasMessageContaining("Crawl interrupted provider=kakao page=1")
                          .hasCauseInstanceOf(InterruptedException.class);

                Assertions.assertThat(Thread.interrupted()).isTrue();
                Mockito.verify(chromeDrivers.constructed().get(0)).quit();
            } finally {
                Thread.interrupted();
            }
            Assertions.assertThat(userDataDirs.get(0)).doesNotExist();
        }
    }
}
