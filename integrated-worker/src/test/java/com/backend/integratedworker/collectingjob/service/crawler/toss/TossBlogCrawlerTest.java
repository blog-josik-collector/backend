package com.backend.integratedworker.collectingjob.service.crawler.toss;

import com.backend.commondataaccess.persistence.provider.PostProvider;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@DisplayName("TossBlogCrawler 테스트")
@ExtendWith(MockitoExtension.class)
class TossBlogCrawlerTest {

    @InjectMocks
    private TossBlogCrawler tossBlogCrawler;

    @Mock
    private TossPostParser tossPostParser;

    @Test
    void baseUrl에_page_쿼리파라미터를_붙여_크롤링_URL을_만든다() {
        PostProvider postProvider = PostProvider.builder()
                                                .id(UUID.randomUUID())
                                                .name("toss")
                                                .baseUrl("https://toss.tech/tech")
                                                .isUsed(true)
                                                .collectSources(new ArrayList<>())
                                                .build();

        String url = tossBlogCrawler.getCrawlingUrl(postProvider, 3);

        Assertions.assertThat(url).isEqualTo("https://toss.tech/tech?page=3");
    }

    @Test
    void 게시글_목록_선택자를_반환한다() {
        Assertions.assertThat(tossBlogCrawler.getPostSelector()).isEqualTo(By.cssSelector("a[data-log-name=\"item\"][data-log-section_title=\"최신 아티클\"]"));
    }

    @Test
    void parsePost는_TossPostParser에_위임한다() {
        WebElement element = Mockito.mock(WebElement.class);
        TossPost post = TossPost.builder()
                                  .title("test_title")
                                  .url("https://toss.tech/article/1")
                                  .publishedAt(LocalDate.of(2025, 1, 1))
                                  .build();
        Mockito.doReturn(post).when(tossPostParser).parse(element);

        TossPost result = tossBlogCrawler.parsePost(element);

        Assertions.assertThat(result).isSameAs(post);
    }
}
