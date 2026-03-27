package com.javajava.project;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// exclude 속성을 사용하여 데이터소스 자동 설정을 제외합니다.
@SpringBootApplication // (exclude = {DataSourceAutoConfiguration.class}) -> DB연결하기 위해 잠시 주석처리
@EnableScheduling
public class ProjectApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        SpringApplication.run(ProjectApplication.class, args);
    }
}
