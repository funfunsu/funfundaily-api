# module description
web 模块，对应 MVC 的 V 概念，存放视图层的逻辑


# 发布流程

## 本地
###
mvn clean
mvn install
### 上传jar包
scp -i ~/Documents/funfundaily/ecs/funfun-ecs.pem schedule-start/target/schedule-start-0.0.1-SNAPSHOT.jar root@120.24.30.25:/root/stack/app/

## 远程服务器重新部署 schedule-app
ssh -i ~/Documents/funfundaily/ecs/funfun-ecs.pem root@120.24.30.25
cd stack/
docker-compose up --build schedule-app




