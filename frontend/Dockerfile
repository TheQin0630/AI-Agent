# 构建阶段：从源码生成静态资源，不依赖本地 dist 目录。
FROM node:20-alpine AS builder

WORKDIR /build

COPY frontend/ ./

RUN npm install --no-audit --no-fund && npm run build

# 运行阶段
FROM nginx:alpine

COPY --from=builder /build/dist /usr/share/nginx/html/
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
