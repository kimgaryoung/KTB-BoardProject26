# syntax=docker/dockerfile:1
# 기능 : 개선 후 멀티 스테이지 / 설명 : 같은 테스트·빌드를 실행하고 JRE 실행 이미지에는 결과 JAR만 복사합니다.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY src/ ./
RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew clean test bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/build/libs/*-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
