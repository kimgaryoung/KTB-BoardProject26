# syntax=docker/dockerfile:1
# 기능 : 개선 후 멀티 스테이지 / 설명 : 의존성 레이어를 소스와 분리하고 검증된 실행 파일만 최종 이미지에 복사합니다.
FROM node:22-alpine AS build
WORKDIR /build
COPY src/package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --omit=dev
COPY src/ ./
RUN npm test

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production PORT=8080
COPY --from=build --chown=node:node /build/node_modules ./node_modules
COPY --from=build --chown=node:node /build/package*.json /build/app.js ./
COPY --from=build --chown=node:node /build/api ./api
COPY --from=build --chown=node:node /build/component ./component
COPY --from=build --chown=node:node /build/css ./css
COPY --from=build --chown=node:node /build/html ./html
COPY --from=build --chown=node:node /build/js ./js
COPY --from=build --chown=node:node /build/utils ./utils
COPY --from=build --chown=node:node /build/public ./public
USER node
EXPOSE 8080
CMD ["node", "app.js"]
