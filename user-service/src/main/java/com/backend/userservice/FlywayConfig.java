//package com.backend.userservice;
//
//import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 1. Flyway 전용 빈(Bean) 설정 (가장 권장) Spring Boot가 실행될 때 자동으로 repair()를 호출하도록 설정 클래스를 추가하는 방법입니다.
// * <p>
// * 이 빈을 등록하고 프로젝트를 실행하면 DB의 체크섬이 현재 로컬 파일 기준으로 업데이트됩니다. 해결 후에는 이 코드를 지우거나 주석 처리하시면 됩니다.
// */
//@Configuration
//public class FlywayConfig {
//
//    @Bean
//    public FlywayMigrationStrategy cleanMigrationStrategy() {
//        return flyway -> {
//            flyway.repair(); // 체크섬 미스매치 해결
//            flyway.migrate(); // 이후 마이그레이션 진행
//        };
//    }
//}
