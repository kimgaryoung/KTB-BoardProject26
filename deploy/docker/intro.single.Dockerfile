# 기능 : 단일 스테이지 인트로 / 설명 : 정적 페이지 변환 도구와 원본도 실행 이미지에 포함하는 비교 버전입니다.
FROM nginx:1.28-alpine
RUN apk add --no-cache python3
COPY src/ /intro-src/
COPY build_intro.py /build_intro.py
RUN python3 /build_intro.py /intro-src /usr/share/nginx/html
COPY intro.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
