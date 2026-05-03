package com.backend.integratedworker.collectingjob.service.crawler.strategy;

import com.backend.commondataaccess.persistence.provider.PostProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public interface CrawlerStrategy<T> {

    String getCrawlingUrl(PostProvider postProvider, int page);

    By getPostSelector();

    T parsePost(WebElement element);
}
