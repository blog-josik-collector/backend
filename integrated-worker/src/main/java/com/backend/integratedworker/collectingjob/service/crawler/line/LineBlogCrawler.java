package com.backend.integratedworker.collectingjob.service.crawler.line;

import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.service.crawler.strategy.CrawlerStrategy;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class LineBlogCrawler implements CrawlerStrategy<LinePost> {

    private final LinePostParser linePostParser;

    @Override
    public String getCrawlingUrl(PostProvider postProvider, int page) {
        return UriComponentsBuilder.fromUriString(postProvider.baseUrl())
                                   .pathSegment("page", String.valueOf(page))
                                   .build()
                                   .toUriString();
    }

    @Override
    public By getPostSelector() {
        return By.cssSelector(".list_post .list_item");
    }

    @Override
    public LinePost parsePost(WebElement element) {
        return linePostParser.parse(element);
    }
}
