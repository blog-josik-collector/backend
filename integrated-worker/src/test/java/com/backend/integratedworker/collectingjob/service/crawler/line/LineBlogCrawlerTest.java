package com.backend.integratedworker.collectingjob.service.crawler.line;

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

@DisplayName("LineBlogCrawler 테스트")
@ExtendWith(MockitoExtension.class)
class LineBlogCrawlerTest {

    @InjectMocks
    private LineBlogCrawler lineBlogCrawler;

    @Mock
    private LinePostParser linePostParser;

    @Test
    void baseUrl에_page_경로를_붙여_크롤링_URL을_만든다() {
        PostProvider postProvider = PostProvider.builder()
                                                .id(UUID.randomUUID())
                                                .name("line")
                                                .baseUrl("https://techblog.lycorp.co.jp/ko")
                                                .isUsed(true)
                                                .collectSources(new ArrayList<>())
                                                .build();

        String url = lineBlogCrawler.getCrawlingUrl(postProvider, 2);

        Assertions.assertThat(url).isEqualTo("https://techblog.lycorp.co.jp/ko/page/2");
    }

    @Test
    void 게시글_목록_선택자를_반환한다() {
        Assertions.assertThat(lineBlogCrawler.getPostSelector()).isEqualTo(By.cssSelector(".list_post .list_item"));
    }

    @Test
    void parsePost는_LinePostParser에_위임한다() {
        WebElement element = Mockito.mock(WebElement.class);
        LinePost post = LinePost.builder()
                                .title("test_title")
                                .url("https://techblog.lycorp.co.jp/ko/post-1")
                                .publishedAt(LocalDate.of(2025, 1, 1))
                                .build();
        Mockito.doReturn(post).when(linePostParser).parse(element);

        LinePost result = lineBlogCrawler.parsePost(element);

        Assertions.assertThat(result).isSameAs(post);
    }
}
