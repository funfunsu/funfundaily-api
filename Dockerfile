# Temurin JDK 17（Spring Boot 3 / Java 17）。
# 国内云服务器拉不到 Docker Hub，改用国内镜像前缀；如该源不可用，可换成下面任一：
#   FROM docker.1panel.live/library/eclipse-temurin:17-jre
#   FROM dockerproxy.cn/library/eclipse-temurin:17-jre
#   FROM eclipse-temurin:17-jre            # 配置了 registry-mirrors 加速器时用这个
FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre

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
