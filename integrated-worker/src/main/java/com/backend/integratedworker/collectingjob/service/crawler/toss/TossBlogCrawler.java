package com.backend.integratedworker.collectingjob.service.crawler.toss;

import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.service.crawler.strategy.CrawlerStrategy;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class TossBlogCrawler implements CrawlerStrategy<TossPost> {

    private final TossPostParser tossPostParser;

    @Override
    public String getCrawlingUrl(PostProvider postProvider, int page) {
        return UriComponentsBuilder.fromUriString(postProvider.baseUrl())
                                   .queryParam("page", page)
                                   .build()
                                   .toUriString();
    }

    @Override
    public By getPostSelector() {
        return By.cssSelector("a[data-log-name=\"item\"][data-log-section_title=\"최신 아티클\"]");
    }

    @Override
    public TossPost parsePost(WebElement element) {
        return tossPostParser.parse(element);
    }
}
