package com.javajava.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// exclude 속성을 사용하여 데이터소스 자동 설정을 제외합니다.
@SpringBootApplication //(exclude = {DataSourceAutoConfiguration.class}) -> DB연결하기 위해 잠시 주석처리
public class ProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjectApplication.class, args);
    }
}
