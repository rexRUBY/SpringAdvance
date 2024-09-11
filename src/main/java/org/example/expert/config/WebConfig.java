package org.example.expert.config;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.aop.AdminAccessLogging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    // ArgumentResolver 등록
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthUserArgumentResolver());
    }

    // Aspect 클래스 등록
    @Bean
    public AdminAccessLogging loggingAspect() {
        return new AdminAccessLogging();
    }
}
