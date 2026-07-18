# 运行镜像：Jar 必须先由本机 Maven 构建并通过测试。
# 这样 Docker 构建不再依赖容器内 Maven 仓库网络。
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
COPY .mvn-settings.xml /root/.m2/settings.xml

RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

# 复制已验证的构建产物
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
