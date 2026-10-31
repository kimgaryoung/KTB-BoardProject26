# 기능 : 독립 멀티 스테이지 인트로 / 설명 : 정적 변환은 빌더에서 하고 Nginx 이미지에는 결과물만 배포합니다.
FROM python:3.12-alpine AS build
COPY src/ /src/
COPY build_intro.py /build_intro.py
RUN python3 /build_intro.py /src /site

FROM nginx:1.28-alpine
COPY --from=build /site/ /usr/share/nginx/html/
COPY intro.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
