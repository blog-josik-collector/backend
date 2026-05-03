package com.backend.integratedworker.collectingjob.service.crawler.kakao;

import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.service.crawler.strategy.CrawlerStrategy;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class KakaoBlogCrawler implements CrawlerStrategy<KakaoPost> {

    private final KakaoPostParser kakaoPostParser;

    @Override
    public String getCrawlingUrl(PostProvider postProvider, int page) {
        return UriComponentsBuilder.fromUriString(postProvider.baseUrl())
                                   .queryParam("page", page)
                                   .build()
                                   .toUriString();
    }

    @Override
    public By getPostSelector() {
        return By.cssSelector("ul.list_post li");
    }

    @Override
    public KakaoPost parsePost(WebElement element) {
        return kakaoPostParser.parse(element);
    }
}
