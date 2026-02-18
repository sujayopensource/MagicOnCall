package com.magiconcall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.magiconcall")
@EnableScheduling
public class MagicOnCallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MagicOnCallApplication.class, args);
    }
}
