package com.imooc.bilibili.service.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // 微服务架构下，CORS 统一由 Gateway 处理，下游服务不再配置跨域
    // 避免 Gateway 和下游服务重复添加 CORS 响应头导致浏览器报错
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("*")
//                .allowedMethods("*")
//                .allowedHeaders("*")
//                .exposedHeaders("*");
//    }

}
