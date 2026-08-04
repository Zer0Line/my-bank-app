package ru.yandex.practicum.mybankfront;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FrontendServiceAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendServiceAppApplication.class, args);
    }
}
