FROM node:22-alpine AS build
WORKDIR /repo

COPY package.json package-lock.json turbo.json tsconfig.base.json ./
COPY apps/web/package.json apps/web/package.json
COPY packages/api/package.json packages/api/package.json
COPY packages/hooks/package.json packages/hooks/package.json
COPY packages/store/package.json packages/store/package.json
RUN npm ci

COPY . .
RUN npx turbo run build --filter=./apps/web

FROM nginx:alpine
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /repo/apps/web/dist /usr/share/nginx/html

EXPOSE 80
