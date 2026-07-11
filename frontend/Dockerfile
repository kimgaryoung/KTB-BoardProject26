# syntax=docker/dockerfile:1
# escape=\

FROM node:22-alpine

ENV WORKDIR=/app \
    NODE_ENV=production \
    PORT=8080

COPY --chown=node:node package.json package-lock.json ./

RUN npm ci --omit=dev \
    && npm cache clean --force

COPY --chown=node:node . .

USER node

EXPOSE 8080

CMD ["node", "app.js"]
