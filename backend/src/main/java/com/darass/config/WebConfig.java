package com.darass.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000", "https://localhost:3000",
                "http://localhost:3001", "https://localhost:3001",
                "https://darass.co.kr", "https://reply-module.darass.co.kr",
                "https://dev.darass.co.kr", "https://reply-module.dev.darass.co.kr")
            .allowCredentials(true)
            .allowedMethods("*");
    }
}
