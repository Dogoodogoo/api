plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.github.dogoodogoo"
version = "0.0.1-SNAPSHOT"
description = "api"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- 핵심 비즈니스 로직 ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Jackson 라이브러리를 명시적으로 추가하여 IDE 인식 오류를 방지합니다.
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // PostGIS의 공간 데이터(Point, LineString)를 Java 객체와 매핑하기 위한 필수 라이브러리입니다.
    implementation("org.hibernate.orm:hibernate-spatial")

    // OWASP A03(Injection) 방어를 위해 사용자의 입력값(좌표 등)을 어노테이션 레벨에서 검증합니다.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Swagger UI 및 OpenAPI 3.0 자동 생성 라이브러리
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // --- 인프라 및 도구 ---
    //application-secret.yml의 데이터를 객체로 변환하는 메타데이터를 컴파일 타임에 생성합니다.
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    //반복적인 코드 생성을 자동화하여 코드의 가독성과 유지보수성을 향상시키는 도구입니다.
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")

    // --- 테스트 환경 ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
