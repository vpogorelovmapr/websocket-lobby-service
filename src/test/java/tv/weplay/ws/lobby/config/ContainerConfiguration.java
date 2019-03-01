package tv.weplay.ws.lobby.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.sql.DataSource;
import java.io.File;

@Configuration
public class ContainerConfiguration {

    private static final String REDIS_WAIT_LOG = ".*Ready to accept connections.*\\s";
    private static final String EUREKA_WAIT_LOG = ".*Started EurekaApplication.*\\s";
    private static final String RABBITMQ_WAIT_LOG = ".*Server startup complete.*\\s";
    private static final String MYSQL_WAIT_LOG = ".*ready for connections.*\\s";

    @Bean(initMethod = "start", destroyMethod = "stop")
    public DockerComposeContainer containers() {
        return new DockerComposeContainer(new File("src/test/resources/docker-compose-test.yml"))
                .withExposedService("redis", 16379, Wait.forLogMessage(REDIS_WAIT_LOG, 1))
                .withExposedService("eureka-service", 18761, Wait.forLogMessage(EUREKA_WAIT_LOG, 1))
                .withExposedService("rabbitmq", 15673, Wait.forLogMessage(RABBITMQ_WAIT_LOG, 1))
                .withExposedService("mysql", 13306, Wait.forLogMessage(MYSQL_WAIT_LOG, 1))
                .withLocalCompose(true);
    }

    @Bean
    @DependsOn("containers")
    public DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder.create()
                .driverClassName(properties.getDriverClassName())
                .url(properties.getUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();
    }

}
