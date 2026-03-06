package com.yupi.babystepaicodeuser;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
@MapperScan("com.yupi.babystepaicodeuser.mapper")
@ComponentScan("com.yupi")
public class BabystepAiCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(BabystepAiCodeUserApplication.class, args);
    }
}
