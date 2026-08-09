package com.backend.commonweb.config;

import com.backend.commondataaccess.security.CurrentUserArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 역할: 공통 웹 계층의 argument resolver 를 모든 서비스 모듈에 등록한다. <p>
 * 모든 애플리케이션이 scanBasePackages = "com.backend" 로 스캔하므로 이 설정 하나로 3개 웹 모듈에 함께 적용된다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
