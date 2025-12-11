FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/catalog-service.jar app.jar
EXPOSE 8082
# 限制JVM内存使用
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]
