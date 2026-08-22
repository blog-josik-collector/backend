package com.backend.integratedworker.collectingjob.service.crawler;

import com.backend.commondataaccess.exception.CrawlingException;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoBlogCrawler;
import com.backend.integratedworker.collectingjob.service.crawler.line.LineBlogCrawler;
import com.backend.integratedworker.collectingjob.service.crawler.strategy.CrawlerStrategy;
import com.backend.integratedworker.collectingjob.service.crawler.toss.TossBlogCrawler;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BlogCrawlerService {

    private final KakaoBlogCrawler kakaoBlogCrawler;
    private final LineBlogCrawler lineBlogCrawler;
    private final TossBlogCrawler tossBlogCrawler;

    public List<Post> fetch(CollectingJob collectingJob) {
        PostProvider postProvider = collectingJob.collectSource().postProvider();
        CrawlerStrategy<? extends Post> strategy = resolveStrategy(postProvider);

        List<Post> posts = new ArrayList<>();
        for (int page = collectingJob.fromPage(); page <= collectingJob.toPage(); page++) {
            posts.addAll(crawlPosts(strategy, postProvider, page));
        }
        return posts;
    }

    private CrawlerStrategy<? extends Post> resolveStrategy(PostProvider postProvider) {
        return switch (postProvider.name()) {
            case "kakao" -> kakaoBlogCrawler;
            case "line" -> lineBlogCrawler;
            case "toss" -> tossBlogCrawler;
            default -> throw new CrawlingException("Unsupported post provider: " + postProvider.name());
        };
    }

    private List<Post> crawlPosts(CrawlerStrategy<? extends Post> strategy, PostProvider postProvider, int page) {
        return new ArrayList<>(crawl(strategy, postProvider, page));
    }

    public <T> List<T> crawl(CrawlerStrategy<T> strategy, PostProvider postProvider, int page) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        String chromeBinary = System.getenv("CHROME_BINARY");
        if (chromeBinary != null && !chromeBinary.isBlank()) {
            options.setBinary(chromeBinary);
        }

        String chromeDriver = System.getenv("CHROME_DRIVER");
        if (chromeDriver != null && !chromeDriver.isBlank()) {
            System.setProperty("webdriver.chrome.driver", chromeDriver);
        } else {
            WebDriverManager.chromedriver().setup();
        }

        WebDriver driver = new ChromeDriver(options);

        List<T> posts = new ArrayList<>();

        try {
            driver.get(strategy.getCrawlingUrl(postProvider, page)); // 지정된 URL로 페이지 이동(블로그 페이지 접속)

            Thread.sleep(5000); // JavaScript 렌더링을 위해 5초 대기

            List<WebElement> elements = driver.findElements(strategy.getPostSelector());  // 특정 선택자에 매칭되는 모든 요소 찾기

            for (WebElement element : elements) {
                T post = strategy.parsePost(element);
                posts.add(post);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CrawlingException(
                    "Crawl interrupted provider=%s page=%d".formatted(postProvider.name(), page), e);
        } catch (Exception e) {
            throw new CrawlingException(
                    "Crawl failed provider=%s page=%d".formatted(postProvider.name(), page), e);
        } finally {
            driver.quit();
        }

        return posts;
    }
}
