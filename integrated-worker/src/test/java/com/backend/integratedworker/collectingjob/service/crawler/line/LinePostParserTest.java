package com.backend.integratedworker.collectingjob.service.crawler.line;

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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

@DisplayName("LinePostParser 테스트")
@ExtendWith(MockitoExtension.class)
class LinePostParserTest {

    @InjectMocks
    private LinePostParser linePostParser;

    /**
     * <a href="{href}">
     *   <div class="thumbnail"><img src="{src}"></div>
     *   <h2 class="title">{title}</h2>
     *   <span class="update">{date}</span>
     * </a>
     */
    private WebElement newLinkElement(String href, String title, String date) {
        WebElement link = Mockito.mock(WebElement.class);
        WebElement titleElement = Mockito.mock(WebElement.class);
        WebElement dateElement = Mockito.mock(WebElement.class);

        Mockito.doReturn(href).when(link).getAttribute("href");
        Mockito.doReturn(titleElement).when(link).findElement(By.cssSelector(".title"));
        Mockito.doReturn(title).when(titleElement).getText();
        Mockito.doReturn(dateElement).when(link).findElement(By.cssSelector(".update"));
        Mockito.doReturn(date).when(dateElement).getText();
        return link;
    }

    @DisplayName("LinePost 파싱 테스트")
    @Nested
    class ParseTest {

        @Test
        void 목록_아이템_하위의_a태그에서_상대경로_href와_썸네일을_파싱한다() {
            // given
            WebElement post = Mockito.mock(WebElement.class);
            WebElement link = newLinkElement("/ko/blog/line-post", "LINE 기술 블로그 글", "2025.02.03");
            WebElement img = Mockito.mock(WebElement.class);

            Mockito.doReturn("div").when(post).getTagName();
            Mockito.doReturn(link).when(post).findElement(By.cssSelector("a"));
            Mockito.doReturn(img).when(link).findElement(By.cssSelector(".thumbnail img"));
            Mockito.doReturn("https://techblog.lycorp.co.jp/thumb.png").when(img).getAttribute("src");

            // when
            LinePost result = linePostParser.parse(post);

            // then
            Assertions.assertThat(result.getTitle()).isEqualTo("LINE 기술 블로그 글");
            Assertions.assertThat(result.getUrl()).isEqualTo("https://techblog.lycorp.co.jp/ko/blog/line-post");
            Assertions.assertThat(result.getPublishedAt()).isEqualTo(LocalDate.of(2025, 2, 3));
            Assertions.assertThat(result.getThumbnailUrl()).contains("https://techblog.lycorp.co.jp/thumb.png");
            Assertions.assertThat(result.getSummary()).isEmpty();
        }

        @Test
        void 요소_자체가_a태그이면_그대로_사용하고_썸네일이_없으면_empty이다() {
            // given
            WebElement link = newLinkElement("https://techblog.lycorp.co.jp/ko/blog/abs", "title", "2024-11-30");
            Mockito.doReturn("A").when(link).getTagName();
            Mockito.doThrow(new NoSuchElementException("no thumbnail"))
                   .when(link).findElement(By.cssSelector(".thumbnail img"));

            // when
            LinePost result = linePostParser.parse(link);

            // then
            Assertions.assertThat(result.getUrl()).isEqualTo("https://techblog.lycorp.co.jp/ko/blog/abs");
            Assertions.assertThat(result.getPublishedAt()).isEqualTo(LocalDate.of(2024, 11, 30));
            Assertions.assertThat(result.getThumbnailUrl()).isEmpty();
        }

        @Test
        void WebElement가_아니면_CrawlingException을_던진다() {
            Assertions.assertThatThrownBy(() -> linePostParser.parse(new Object()))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("expects WebElement");
        }

        @Test
        void href가_없으면_CrawlingException을_던진다() {
            // given
            WebElement link = Mockito.mock(WebElement.class);
            Mockito.doReturn("a").when(link).getTagName();
            Mockito.doReturn(null).when(link).getAttribute("href");

            // when & then
            Assertions.assertThatThrownBy(() -> linePostParser.parse(link))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("href attribute not found");
        }
    }

    @DisplayName("발행일 파싱 테스트")
    @Nested
    class ParsePublishedAtTest {

        @Test
        void 점_포맷_날짜가_포함되어_있으면_추출해서_변환한다() {
            Assertions.assertThat(linePostParser.parsePublishedAt("업데이트: 2025.04.05"))
                      .isEqualTo(LocalDate.of(2025, 4, 5));
        }

        @Test
        void 대시_포맷_날짜가_포함되어_있으면_추출해서_변환한다() {
            Assertions.assertThat(linePostParser.parsePublishedAt("Updated 2025-04-05"))
                      .isEqualTo(LocalDate.of(2025, 4, 5));
        }

        @Test
        void 영문_RFC_포맷_날짜를_변환한다() {
            Assertions.assertThat(linePostParser.parsePublishedAt("  Sat, 05 Apr 2025 10:00:00 GMT  "))
                      .isEqualTo(LocalDate.of(2025, 4, 5));
        }

        @Test
        void 지원하지_않는_포맷이면_CrawlingException을_던진다() {
            Assertions.assertThatThrownBy(() -> linePostParser.parsePublishedAt("3 days ago"))
                      .isInstanceOf(CrawlingException.class)
                      .hasMessageContaining("Unsupported LINE blog date format");
        }
    }
}
