# 기능 : 개선 전 단일 스테이지 / 설명 : 의존성 설치와 동일 테스트를 수행하고 소스·테스트를 함께 보관합니다.
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production PORT=8080
COPY src/ ./
RUN npm ci --omit=dev && npm test
USER node
EXPOSE 8080
CMD ["node", "app.js"]
