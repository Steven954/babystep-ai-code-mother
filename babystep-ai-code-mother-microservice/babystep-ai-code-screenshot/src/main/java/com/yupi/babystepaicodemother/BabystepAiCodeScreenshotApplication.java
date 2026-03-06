package com.yupi.babystepaicodemother;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class BabystepAiCodeScreenshotApplication {
    public static void main(String[] args) {
        SpringApplication.run(BabystepAiCodeScreenshotApplication.class, args);
    }
}
