package com.backend.integratedworker.collectingjob.service.crawler.line;

import com.backend.commondataaccess.exception.CrawlingException;
import com.backend.integratedworker.collectingjob.service.crawler.strategy.PostParser;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class LinePostParser implements PostParser<LinePost> {

    private static final String BASE_URL = "https://techblog.lycorp.co.jp";
    private static final DateTimeFormatter YYYY_MM_DD_WITH_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DOT_DATE_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})");
    private static final Pattern DASH_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern RFC_DATE_PATTERN = Pattern.compile("(?i)([A-Za-z]{3}, \\d{1,2} [A-Za-z]{3} \\d{4})");
    private static final DateTimeFormatter RFC_DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);

    @Override
    public LinePost parse(Object rawData) {
        if (!(rawData instanceof WebElement)) {
            throw new CrawlingException("LinePost expects WebElement");
        }

        WebElement post = (WebElement) rawData;
        WebElement linkElement = resolveLinkElement(post);

        String href = linkElement.getAttribute("href");

        if (href == null) {
            throw new CrawlingException("LinePost href attribute not found");
        }

        String url = href.startsWith("http") ? href : BASE_URL + href;

        String title = linkElement.findElement(By.cssSelector(".title")).getText();
        String publishedAt = linkElement.findElement(By.cssSelector(".update")).getText();

        Optional<String> thumbnailUrl = findThumbnailUrl(linkElement);

        return LinePost.builder()
                       .title(title)
                       .url(url)
                       .publishedAt(parsePublishedAt(publishedAt))
                       .thumbnailUrl(thumbnailUrl)
                       .summary(Optional.empty())
                       .build();
    }

    @Override
    public LocalDate parsePublishedAt(String metaData) {
        try {
            Matcher dotDateMatcher = DOT_DATE_PATTERN.matcher(metaData);
            if (dotDateMatcher.find()) {
                return LocalDate.parse(dotDateMatcher.group(1), YYYY_MM_DD);
            }

            Matcher dashDateMatcher = DASH_DATE_PATTERN.matcher(metaData);
            if (dashDateMatcher.find()) {
                return LocalDate.parse(dashDateMatcher.group(1), YYYY_MM_DD_WITH_DASH);
            }

            Matcher rfcDateMatcher = RFC_DATE_PATTERN.matcher(metaData.trim());
            if (rfcDateMatcher.lookingAt()) {
                return LocalDate.parse(rfcDateMatcher.group(1), RFC_DATE);
            }
        } catch (DateTimeParseException e) {
            throw new CrawlingException("Invalid LINE blog date: " + metaData, e);
        }

        throw new CrawlingException("Unsupported LINE blog date format: " + metaData);
    }

    private WebElement resolveLinkElement(WebElement post) {
        if ("a".equalsIgnoreCase(post.getTagName())) {
            return post;
        }

        return post.findElement(By.cssSelector("a"));
    }

    private Optional<String> findThumbnailUrl(WebElement linkElement) {
        try {
            WebElement img = linkElement.findElement(By.cssSelector(".thumbnail img"));
            return Optional.ofNullable(img.getAttribute("src"));
        } catch (NoSuchElementException ignored) {
            return Optional.empty();
        }
    }
}
