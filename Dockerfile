# 使用官方 OpenJDK 11 镜像（根据你项目 JDK 版本调整）
FROM openjdk:11-jre-slim

# 设置时区（可选但推荐）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用目录
WORKDIR /app

# 复制 JAR 文件（注意：Maven 构建后 JAR 在 target/ 下）
COPY schedule-start-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口（根据你 application.yml 中 server.port 设置，如 8080）
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-XX:+PrintGCDetails", "-jar", "app.jar", "--debug"]
