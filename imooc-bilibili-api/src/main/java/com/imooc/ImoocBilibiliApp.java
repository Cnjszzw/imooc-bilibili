package com.imooc;


import com.imooc.bilibili.service.websocket.WebSocketService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class ImoocBilibiliApp {
    public static void main(String[] args) {
        ApplicationContext app = SpringApplication.run(ImoocBilibiliApp.class, args);
        WebSocketService.setApplicationContext(app);
    }
}
