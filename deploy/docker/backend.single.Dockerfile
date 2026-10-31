# 기능 : 개선 전 단일 스테이지 / 설명 : 테스트·빌드 도구와 소스를 같은 실행 이미지에 남깁니다.
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY src/ ./
RUN chmod +x gradlew && ./gradlew clean test bootJar --no-daemon && cp build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
