package com.backend.integratedworker.collectingjob.service.crawler.kakao;

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

@DisplayName("KakaoBlogCrawler 테스트")
@ExtendWith(MockitoExtension.class)
class KakaoBlogCrawlerTest {

    @InjectMocks
    private KakaoBlogCrawler kakaoBlogCrawler;

    @Mock
    private KakaoPostParser kakaoPostParser;

    @Test
    void baseUrl에_page_쿼리파라미터를_붙여_크롤링_URL을_만든다() {
        PostProvider postProvider = PostProvider.builder()
                                                .id(UUID.randomUUID())
                                                .name("kakao")
                                                .baseUrl("https://tech.kakao.com/blog")
                                                .isUsed(true)
                                                .collectSources(new ArrayList<>())
                                                .build();

        String url = kakaoBlogCrawler.getCrawlingUrl(postProvider, 3);

        Assertions.assertThat(url).isEqualTo("https://tech.kakao.com/blog?page=3");
    }

    @Test
    void 게시글_목록_선택자를_반환한다() {
        Assertions.assertThat(kakaoBlogCrawler.getPostSelector()).isEqualTo(By.cssSelector("ul.list_post li"));
    }

    @Test
    void parsePost는_KakaoPostParser에_위임한다() {
        WebElement element = Mockito.mock(WebElement.class);
        KakaoPost post = KakaoPost.builder()
                                  .title("test_title")
                                  .url("https://tech.kakao.com/posts/1")
                                  .publishedAt(LocalDate.of(2025, 1, 1))
                                  .build();
        Mockito.doReturn(post).when(kakaoPostParser).parse(element);

        KakaoPost result = kakaoBlogCrawler.parsePost(element);

        Assertions.assertThat(result).isSameAs(post);
    }
}
