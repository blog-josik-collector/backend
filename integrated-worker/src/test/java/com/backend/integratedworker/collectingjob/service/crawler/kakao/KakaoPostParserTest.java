package com.backend.integratedworker.collectingjob.service.crawler.kakao;

import com.backend.commondataaccess.exception.CrawlingException;
import java.time.LocalDate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@DisplayName("KakaoPostParser 테스트")
@ExtendWith(MockitoExtension.class)
class KakaoPostParserTest {

    @InjectMocks
    private KakaoPostParser kakaoPostParser;

    /**
     * <li>
     *   <a class="link_post" href="{href}">
     *     <div class="box_thumb"><img class="img_thumbnail" src="{src}"></div>
     *     <h4 class="tit_post">{title}</h4>
     *     <dd class="txt_date">{date}</dd>
     *   </a>
     * </li>
     */
    private WebElement newPostElement(String href, String title, String date, String src) {
        WebElement post = Mockito.mock(WebElement.class);
        WebElement link = Mockito.mock(WebElement.class);
        WebElement titleElement = Mockito.mock(WebElement.class);
        WebElement dateElement = Mockito.mock(WebElement.class);
        WebElement img = Mockito.mock(WebElement.class);

        Mockito.doReturn(link).when(post).findElement(By.cssSelector("a.link_post"));
        Mockito.doReturn(href).when(link).getAttribute("href");
        Mockito.doReturn(titleElement).when(link).findElement(By.cssSelector("h4.tit_post"));
        Mockito.doReturn(title).when(titleElement).getText();
        Mockito.doReturn(dateElement).when(link).findElement(By.cssSelector("dd.txt_date"));
        Mockito.doReturn(date).when(dateElement).getText();
        Mockito.doReturn(img).when(link).findElement(By.cssSelector("div.box_thumb img.img_thumbnail"));
        Mockito.doReturn(src).when(img).getAttribute("src");
        return post;
    }

    @DisplayName("KakaoPost 파싱 테스트")
    @Nested
    class ParseTest {

        @Test
        void 상대경로_href는_tech_kakao_도메인을_붙여_KakaoPost로_파싱한다() {
            // given
            WebElement post = newPostElement("/posts/700", "카카오 테크 포스트", "2025.03.15",
                                             "https://t1.kakaocdn.net/thumb.png");

            // when
            KakaoPost result = kakaoPostParser.parse(post);

            // then
            Assertions.assertThat(result.getTitle()).isEqualTo("카카오 테크 포스트");
            Assertions.assertThat(result.getUrl()).isEqualTo("https://tech.kakao.com/posts/700");
            Assertions.assertThat(result.getPublishedAt()).isEqualTo(LocalDate.of(2025, 3, 15));
            Assertions.assertThat(result.getThumbnailUrl()).contains("https://t1.kakaocdn.net/thumb.png");
            Assertions.assertThat(result.getSummary()).isEmpty();
        }

        @Test
        void 절대경로_href는_그대로_사용하고_썸네일_src가_없으면_empty이다() {
            // given
            WebElement post = newPostElement("https://tech.kakao.com/posts/701", "title", "2024.12.01", null);

            // when
            KakaoPost result = kakaoPostParser.parse(post);

            // then
            Assertions.assertThat(result.getUrl()).isEqualTo("https://tech.kakao.com/posts/701");
            Assertions.assertThat(result.getThumbnailUrl()).isEmpty();
        }

        @Test
        void WebElement가_아니면_CrawlingException을_던진다() {
            Assertions.assertThatThrownBy(() -> kakaoPostParser.parse("<li>not element</li>"))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("expects WebElement");
        }

        @Test
        void href가_없으면_CrawlingException을_던진다() {
            // given
            WebElement post = Mockito.mock(WebElement.class);
            WebElement link = Mockito.mock(WebElement.class);
            Mockito.doReturn(link).when(post).findElement(By.cssSelector("a.link_post"));
            Mockito.doReturn(null).when(link).getAttribute("href");

            // when & then
            Assertions.assertThatThrownBy(() -> kakaoPostParser.parse(post))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("href attribute not found");
        }
    }

    @DisplayName("발행일 파싱 테스트")
    @Nested
    class ParsePublishedAtTest {

        @Test
        void yyyy_MM_dd_점_포맷을_LocalDate로_변환한다() {
            Assertions.assertThat(kakaoPostParser.parsePublishedAt("2025.01.02")).isEqualTo(LocalDate.of(2025, 1, 2));
        }

        @Test
        void 포맷이_다르면_CrawlingException을_던진다() {
            Assertions.assertThatThrownBy(() -> kakaoPostParser.parsePublishedAt("2025-01-02"))
                      .isInstanceOf(CrawlingException.class);
        }
    }
}
