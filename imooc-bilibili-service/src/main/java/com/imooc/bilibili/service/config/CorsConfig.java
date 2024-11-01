package com.imooc.bilibili.service.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        /**
         * addMapping("/**") 中的路径模式 "/**" 是一种 URL 匹配方式，
         * 用于指定可以处理跨域请求的路径范围。路径模式决定了哪些 URL 可以符合配置的 CORS 规则。
         * 这里的 ** 是一种通配符，表示匹配多级路径。
         */
        registry.addMapping("/**")
                // 是否发送Cookie
                .allowCredentials(true)
                // 放行哪些原始域
                .allowedOrigins("http://localhost:7070","http://localhost:7071","http://localhost:7072")
                // 放行哪些请求方式
                .allowedMethods("*")
                // 放行哪些原始请求头部信息
                .allowedHeaders("*")
                // 暴露哪些头部信息
                .exposedHeaders("*");
    }

}
