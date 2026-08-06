package com.backend.integratedworker.collectingjob.service.crawler.toss;

import com.backend.commondataaccess.exception.CrawlingException;
import com.backend.integratedworker.collectingjob.service.crawler.strategy.PostParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class TossPostParser implements PostParser<TossPost> {

    private static final String BASE_URL = "https://toss.tech";
    private static final Pattern PUBLISHED_TIME_PATTERN = Pattern.compile("publishedTime\\\\\":\\\\\"([^\\\\\"]+)\\\\\"");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Override
    public TossPost parse(Object rawData) {
        if (!(rawData instanceof WebElement)) {
            throw new CrawlingException("TossPost expects WebElement");
        }

        WebElement post = (WebElement) rawData;

        String title = post.getAttribute("data-log-item_title");

        if (title == null) {
            throw new CrawlingException("TossPost title attribute not found");
        }

        String href = post.getAttribute("href");

        if (href == null) {
            throw new CrawlingException("TossPost href attribute not found");
        }

        String url = href.startsWith("http") ? href : BASE_URL + href;

        Optional<String> summary = findSummary(post);
        Optional<String> thumbnailUrl = findThumbnailUrl(post);

        return TossPost.builder()
                       .title(title)
                       .url(url)
                       .publishedAt(fetchPublishedAt(url))
                       .thumbnailUrl(thumbnailUrl)
                       .summary(summary)
                       .build();
    }

    @Override
    public LocalDate parsePublishedAt(String metaData) {
        return OffsetDateTime.parse(metaData).toLocalDate();
    }

    private LocalDate fetchPublishedAt(String articleUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(articleUrl))
                                             .header("User-Agent", "Mozilla/5.0")
                                             .GET()
                                             .build();
            String body = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();

            Matcher matcher = PUBLISHED_TIME_PATTERN.matcher(body);
            if (matcher.find()) {
                return parsePublishedAt(matcher.group(1));
            }

            throw new CrawlingException("Toss article published date not found: " + articleUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CrawlingException("Failed to fetch Toss article published date: " + articleUrl);
        } catch (IOException e) {
            throw new CrawlingException("Failed to fetch Toss article published date: " + articleUrl);
        }
    }

    private Optional<String> findSummary(WebElement post) {
        try {
            List<WebElement> contentDivs = post.findElement(By.xpath("./div[1]/div")).findElements(By.xpath("./div"));
            if (contentDivs.size() < 3) {
                return Optional.empty();
            }

            String summary = contentDivs.get(2).getText().trim();
            return summary.isEmpty() ? Optional.empty() : Optional.of(summary);
        } catch (NoSuchElementException ignored) {
            return Optional.empty();
        }
    }

    private Optional<String> findThumbnailUrl(WebElement post) {
        try {
            WebElement img = post.findElement(By.cssSelector("img"));
            return Optional.ofNullable(img.getAttribute("src"));
        } catch (NoSuchElementException ignored) {
            return Optional.empty();
        }
    }
}
