package com.backend.integratedworker.collectingjob.service.crawler.kakao;

import com.backend.integratedworker.collectingjob.service.crawler.strategy.PostParser;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class KakaoPostParser implements PostParser<KakaoPost> {

    @Override
    public KakaoPost parse(Object rawData) {
        if (!(rawData instanceof WebElement)) {
            throw new IllegalArgumentException("KakaoPost expects WebElement");
        }

        WebElement post = (WebElement) rawData;

        WebElement linkElement = post.findElement(By.cssSelector("a.link_post"));
        String href = linkElement.getAttribute("href");
        String url = Objects.requireNonNull(href).startsWith("http") ? href : "https://tech.kakao.com" + href;

        String title = linkElement.findElement(By.cssSelector("h4.tit_post")).getText();
        String publishedAt = linkElement.findElement(By.cssSelector("dd.txt_date")).getText();

        WebElement img = linkElement.findElement(By.cssSelector("div.box_thumb img.img_thumbnail"));
        Optional<String> thumbnailUrl = Optional.ofNullable(img.getAttribute("src"));

        return KakaoPost.builder()
                        .title(title)
                        .url(url)
                        .publishedAt(parsePublishedAt(publishedAt))
                        .thumbnailUrl(thumbnailUrl)
                        .summary(Optional.empty())
                        .build();
    }

    @Override
    public LocalDate parsePublishedAt(String metaData) {
        // 정해진 날짜 포맷으로 변경 (yyyy.MM.dd)
        return LocalDate.parse(metaData, YYYY_MM_DD);
    }
}
