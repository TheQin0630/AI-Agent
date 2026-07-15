# 阶段 1：构建（Maven + JDK 17）
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先只复制 pom.xml，利用 Docker 缓存加速依赖下载
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY backend/src ./src
RUN mvn package -DskipTests -B -q

# 阶段 2：运行（JRE 17）
FROM eclipse-temurin:17-jre

WORKDIR /app

# 复制构建产物
COPY --from=builder /build/target/*.jar app.jar

# 存储目录
RUN mkdir -p /app/storage/contracts
VOLUME /app/storage

# 时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8080

# 使用 docker profile
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
