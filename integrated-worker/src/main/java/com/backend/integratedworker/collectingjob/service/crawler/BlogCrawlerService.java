package com.backend.integratedworker.collectingjob.service.crawler;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoBlogCrawler;
import com.backend.integratedworker.collectingjob.service.crawler.strategy.CrawlerStrategy;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BlogCrawlerService {

    private final KakaoBlogCrawler kakaoBlogCrawler;

    public List<Post> fetch(CollectSource collectSource) {
        PostProvider postProvider = collectSource.postProvider();
        return new ArrayList<>(crawl(kakaoBlogCrawler, postProvider, 1));
    }

    public <T> List<T> crawl(CrawlerStrategy<T> strategy, PostProvider postProvider, int page) {
        WebDriverManager.chromedriver().setup(); // Chrome WebDriver를 자동으로 다운로드하고 설정

        ChromeOptions options = new ChromeOptions();  // Chrome 브라우저 설정을 위한 객체 생성
        options.addArguments("--headless=new"); // 헤드리스 모드 설정 (브라우저 UI 없이 실행)
        WebDriver driver = new ChromeDriver(options); // 설정된 옵션으로 Chrome 브라우저 인스턴스 생성

        List<T> posts = new ArrayList<>();

        try {
            driver.get(strategy.getCrawlingUrl(postProvider, page)); // 지정된 URL로 페이지 이동(블로그 페이지 접속)

            Thread.sleep(5000); // JavaScript 렌더링을 위해 5초 대기

            List<WebElement> elements = driver.findElements(strategy.getPostSelector());  // 특정 선택자에 매칭되는 모든 요소 찾기

            for (WebElement element : elements) {
                T post = strategy.parsePost(element); // 각 요소를 파싱하여 포스트 객체로 변환
                log.info("post: {}", post); // 포스트 정보 로깅
                posts.add(post); // 포스트 객체를 리스트에 추가
            }
        } catch (Exception e) {
            log.error("Error while crawling: {}", e.getMessage(), e);
        } finally {
            driver.quit(); // 브라우저 인스턴스 종료 및 리소스 정리
        }

        return posts;
    }
}
