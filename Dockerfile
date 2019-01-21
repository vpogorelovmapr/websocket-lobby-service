FROM weplayregistrytest.azurecr.io/java8:1.1

WORKDIR /service
ENV JAVA_OPTS "-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
ENV SERVICE_PARAMS ""
COPY target/websocket-lobby-service.jar /service/
CMD java-entrypoint websocket-lobby-service.jar
