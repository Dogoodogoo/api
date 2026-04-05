# 1단계: 빌드 환경 (라이브러리 캐시 활용)
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# [핵심] 설정 파일을 먼저 복사하여 의존성(Library) 레이어를 캐싱합니다.
# 소스 코드가 바뀌어도 라이브러리가 그대로라면 이 단계는 캐시에서 처리되어 빌드 시간이 단축됩니다.
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle build -x test --parallel --no-daemon > /dev/null 2>&1 || true

# 실제 소스 코드를 복사하고 최종 빌드를 수행합니다.
COPY src ./src
RUN gradle clean build -x test --parallel --no-daemon

# 2단계: 실행 환경 (경량화 배포용 이미지)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 빌드 단계에서 생성된 실행 가능한 JAR 파일만 추출하여 이미지 용량을 최소화합니다.
COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8080

# 서버의 호스트 경로와 마운트된 설정 파일(application-secret.yml)을 로드하며 실행합니다.
ENTRYPOINT ["java", "-jar", "-Dspring.config.import=file:/app/config/application-secret.yml", "app.jar"]