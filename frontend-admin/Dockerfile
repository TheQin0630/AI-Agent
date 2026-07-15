# 阶段 1：构建（Node + npm）
FROM node:20-alpine AS builder

WORKDIR /build

# 复制全部源码（.dockerignore 已排除 node_modules/dist 等）
COPY frontend-admin/ ./

# 用 npm 安装依赖并构建（npm 创建扁平 node_modules，兼容性最好）
RUN npm install --no-audit --no-fund && \
    npx vite build

# 阶段 2：运行（nginx）
FROM nginx:alpine

COPY --from=builder /build/dist /usr/share/nginx/html
COPY frontend-admin/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
