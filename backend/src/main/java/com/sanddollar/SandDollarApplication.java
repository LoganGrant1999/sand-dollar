package com.sanddollar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SandDollarApplication {
    public static void main(String[] args) {
        SpringApplication.run(SandDollarApplication.class, args);
    }
}