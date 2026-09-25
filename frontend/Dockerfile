FROM node:24-bookworm-slim AS build

WORKDIR /app
RUN npm install --global pnpm@11.19.0
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .

ARG VITE_API_URL=http://localhost:8080
ARG VITE_DEMO_MODE=false
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_DEMO_MODE=${VITE_DEMO_MODE}
RUN pnpm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
